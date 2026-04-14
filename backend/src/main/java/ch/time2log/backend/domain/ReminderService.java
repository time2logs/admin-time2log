package ch.time2log.backend.domain;

import ch.time2log.backend.infrastructure.mail.ReminderMailService;
import ch.time2log.backend.infrastructure.supabase.SupabaseAdminClient;
import ch.time2log.backend.infrastructure.supabase.responses.ActivityRecordResponse;
import ch.time2log.backend.infrastructure.supabase.responses.OrganizationMemberResponse;
import ch.time2log.backend.infrastructure.supabase.responses.OrganizationResponse;
import ch.time2log.backend.infrastructure.supabase.responses.ProfileResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Component
public class ReminderService {
    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);
    private static final long INACTIVE_THRESHOLD_DAYS = 3;

    private final SupabaseAdminClient adminClient;
    private final ReminderMailService reminderMailService;

    @Value("${app.url}")
    private String appUrl;

    public ReminderService(SupabaseAdminClient adminClient, ReminderMailService reminderMailService) {
        this.adminClient = adminClient;
        this.reminderMailService = reminderMailService;
    }

    /**
     * Runs every Friday at 08:00 (server time).
     * Sends a reminder email to all users who have not logged an activity record in the last 3 days.
     */
    @Scheduled(cron = "40 46 14 * * MON")
    public void sendWeeklyReminders() {
        log.info("Starting weekly reminder check...");

        var organizations = adminClient.getListWithQuery(
                "admin.organizations",
                "select=id,name",
                OrganizationResponse.class
        );

        for (var org : organizations) {
            processOrganization(org.id(), org.name());
        }

        log.info("Weekly reminder check completed.");
    }

    private void processOrganization(UUID orgId, String orgName) {
        var members = adminClient.getListWithQuery(
                "admin.organization_members",
                "organization_id=eq." + orgId + "&user_role=eq.user",
                OrganizationMemberResponse.class
        );

        if (members.isEmpty()) return;

        var cutoffDate = LocalDate.now().minusDays(INACTIVE_THRESHOLD_DAYS).toString();

        for (var member : members) {
            try {
                checkAndNotifyMember(member.user_id(), orgId, orgName, cutoffDate);
            } catch (Exception e) {
                log.error("Error processing reminder for user {} in org {}: {}", member.user_id(), orgId, e.getMessage());
            }
        }
    }

    private void checkAndNotifyMember(UUID userId, UUID orgId, String orgName, String cutoffDate) {
        var recentRecords = adminClient.getListWithQuery(
                "app.activity_records",
                "organization_id=eq." + orgId + "&user_id=eq." + userId
                        + "&entry_date=gte." + cutoffDate + "&limit=1",
                ActivityRecordResponse.class
        );
        log.info("Checking reminder for user {} in org {}", userId, orgName);
        if (!recentRecords.isEmpty()) return;

        // User has no records in the last 3 days -> determine exact days inactive
        var lastRecords = adminClient.getListWithQuery(
                "app.activity_records",
                "organization_id=eq." + orgId + "&user_id=eq." + userId
                        + "&order=entry_date.desc&limit=1",
                ActivityRecordResponse.class
        );

        long daysInactive;
        if (lastRecords.isEmpty()) {
            daysInactive = INACTIVE_THRESHOLD_DAYS;
        } else {
            var lastDate = LocalDate.parse(lastRecords.getFirst().entry_date());
            daysInactive = ChronoUnit.DAYS.between(lastDate, LocalDate.now());
        }

        // Fetch profile for first name
        var profiles = adminClient.getListWithQuery(
                "app.profiles",
                "id=eq." + userId,
                ProfileResponse.class
        );
        var firstName = profiles.isEmpty() ? "User" : profiles.getFirst().first_name();

        // Fetch email from Supabase Auth
        var email = adminClient.getUserEmail(userId);
        log.info("user email is " + email);
        if (email == null || email.isBlank()) {
            log.warn("No email found for user {}, skipping reminder", userId);
            return;
        }

        reminderMailService.sendReminder(email, firstName, orgName, daysInactive, appUrl);
        log.info("Sent reminder to {} ({}) - {} days inactive in org {}", email, firstName, daysInactive, orgName);
    }
}
