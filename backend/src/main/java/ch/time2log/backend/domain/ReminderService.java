package ch.time2log.backend.domain;

import ch.time2log.backend.infrastructure.mail.ReminderMailService;
import ch.time2log.backend.infrastructure.supabase.SupabaseAdminClient;
import ch.time2log.backend.infrastructure.supabase.responses.ActivityRecordResponse;
import ch.time2log.backend.infrastructure.supabase.responses.OrganizationMemberResponse;
import ch.time2log.backend.infrastructure.supabase.responses.OrganizationResponse;
import ch.time2log.backend.infrastructure.supabase.responses.ProfileResponse;
import ch.time2log.backend.infrastructure.supabase.responses.ReminderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Component
public class ReminderService {
    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    private final SupabaseAdminClient adminClient;
    private final ReminderMailService reminderMailService;

    @Value("${app.url}")
    private String appUrl;

    public ReminderService(SupabaseAdminClient adminClient, ReminderMailService reminderMailService) {
        this.adminClient = adminClient;
        this.reminderMailService = reminderMailService;
    }

    /**
     * Runs every minute to check if any organization has a reminder due right now.
     * Matches the current day and hour/minute against each org's reminder config.
     */
    @Scheduled(cron = "0 * * * * *")
    public void sendWeeklyReminders() {
        log.info("Starting reminder check...");

        var reminders = adminClient.getListWithQuery(
                "admin.reminder",
                "select=*",
                ReminderResponse.class
        );

        if (reminders.isEmpty()) {
            log.info("No reminder configurations found.");
            return;
        }

        var swissZone = ZoneId.of("Europe/Zurich");
        var today = LocalDate.now(swissZone);
        var currentDay = today.getDayOfWeek();
        var currentTime = LocalTime.now(swissZone);

        for (var reminder : reminders) {
            try {
                var sendDay = DayOfWeek.valueOf(reminder.send_day());
                if (currentDay != sendDay) continue;

                var sendTime = LocalTime.parse(reminder.send_time());
                if (currentTime.getHour() != sendTime.getHour() || currentTime.getMinute() != sendTime.getMinute()) continue;

                log.info("Reminder due for org {} (channel={}, day={}, time={})",
                        reminder.organization_id(), reminder.channel(), reminder.send_day(), reminder.send_time());

                processOrganization(reminder.organization_id(), reminder.idle_days(), reminder.channel());
            } catch (Exception e) {
                log.error("Error processing reminder config for org {}: {}", reminder.organization_id(), e.getMessage());
            }
        }

        log.info("Reminder check completed.");
    }

    private void processOrganization(UUID orgId, int idleDays, String channel) {
        var organizations = adminClient.getListWithQuery(
                "admin.organizations",
                "id=eq." + orgId + "&select=id,name",
                OrganizationResponse.class
        );
        if (organizations.isEmpty()) {
            log.warn("Organization {} not found, skipping", orgId);
            return;
        }
        var orgName = organizations.getFirst().name();

        var members = adminClient.getListWithQuery(
                "admin.organization_members",
                "organization_id=eq." + orgId + "&user_role=eq.user",
                OrganizationMemberResponse.class
        );

        if (members.isEmpty()) return;

        var cutoffDate = LocalDate.now().minusDays(idleDays).toString();

        for (var member : members) {
            try {
                checkAndNotifyMember(member.user_id(), orgId, orgName, cutoffDate, idleDays, channel);
            } catch (Exception e) {
                log.error("Error processing reminder for user {} in org {}: {}", member.user_id(), orgId, e.getMessage());
            }
        }
    }

    private void checkAndNotifyMember(UUID userId, UUID orgId, String orgName, String cutoffDate, int idleDays, String channel) {
        var recentRecords = adminClient.getListWithQuery(
                "app.activity_records",
                "organization_id=eq." + orgId + "&user_id=eq." + userId
                        + "&entry_date=gte." + cutoffDate + "&limit=1",
                ActivityRecordResponse.class
        );
        log.info("Checking reminder for user {} in org {}", userId, orgName);
        if (!recentRecords.isEmpty()) return;

        var lastRecords = adminClient.getListWithQuery(
                "app.activity_records",
                "organization_id=eq." + orgId + "&user_id=eq." + userId
                        + "&order=entry_date.desc&limit=1",
                ActivityRecordResponse.class
        );

        long daysInactive;
        if (lastRecords.isEmpty()) {
            daysInactive = idleDays;
        } else {
            var lastDate = LocalDate.parse(lastRecords.getFirst().entry_date());
            daysInactive = ChronoUnit.DAYS.between(lastDate, LocalDate.now());
        }

        var profiles = adminClient.getListWithQuery(
                "app.profiles",
                "id=eq." + userId,
                ProfileResponse.class
        );
        var firstName = profiles.isEmpty() ? "User" : profiles.getFirst().first_name();

        var email = adminClient.getUserEmail(userId);
        if (email == null || email.isBlank()) {
            log.warn("No email found for user {}, skipping reminder", userId);
            return;
        }

        if ("EMAIL".equals(channel)) {
            reminderMailService.sendReminder(email, firstName, orgName, daysInactive, appUrl);
            log.info("Sent EMAIL reminder to {} ({}) - {} days inactive in org {}", email, firstName, daysInactive, orgName);
        } else {
            log.info("SMS reminder for {} ({}) - {} days inactive in org {} (SMS not yet implemented)", email, firstName, daysInactive, orgName);
        }
    }
}
