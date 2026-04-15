package ch.time2log.backend.domain;

import ch.time2log.backend.domain.exception.EntityNotCreatedException;
import ch.time2log.backend.domain.exception.NoRowsAffectedException;
import ch.time2log.backend.domain.models.Invite;
import ch.time2log.backend.domain.models.Organization;
import ch.time2log.backend.infrastructure.mail.InviteMailService;
import ch.time2log.backend.infrastructure.mail.ReminderMailService;
import ch.time2log.backend.infrastructure.sms.ReminderSmsService;
import ch.time2log.backend.infrastructure.supabase.SupabaseAdminClient;
import ch.time2log.backend.infrastructure.supabase.SupabaseApiException;
import ch.time2log.backend.infrastructure.supabase.SupabaseService;
import ch.time2log.backend.infrastructure.supabase.responses.ActivityRecordResponse;
import ch.time2log.backend.infrastructure.supabase.responses.InviteResponse;
import ch.time2log.backend.infrastructure.supabase.responses.OrganizationMemberResponse;
import ch.time2log.backend.infrastructure.supabase.responses.OrganizationResponse;
import ch.time2log.backend.infrastructure.supabase.responses.ProfileResponse;
import ch.time2log.backend.infrastructure.supabase.responses.ReminderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationDomainServiceTest {

    @Mock
    private SupabaseService supabaseService;

    @Mock
    private SupabaseAdminClient supabaseAdminClient;

    @Mock
    private ProfileDomainService profileDomainService;

    @Mock
    private InviteMailService inviteMailService;

    @InjectMocks
    private OrganizationDomainService organizationDomainService;

    private final UUID orgId = UUID.randomUUID();
    private final UUID inviteId = UUID.randomUUID();
    private final UUID invitedBy = UUID.randomUUID();
    private final UUID memberId = UUID.randomUUID();
    private final UUID token = UUID.randomUUID();
    private final String email = "user@example.com";
    private final String userRole = "user";
    private final String actionLink = "https://supabase.example.com/auth/v1/verify?token=abc";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(organizationDomainService, "appUrl", "http://localhost:4300");
    }

    // --- createInvite ---

    @Test
    void createInvite_happyPath_returnsInvite() {
        var inviteResponse = inviteResponse();
        when(supabaseService.post(eq("admin.invites"), any(), eq(InviteResponse[].class)))
                .thenReturn(new InviteResponse[]{inviteResponse});
        when(supabaseService.getListWithQuery(eq("admin.organizations"), anyString(), eq(OrganizationResponse.class)))
                .thenReturn(List.of(orgResponse("Acme Corp")));
        when(supabaseAdminClient.generateInviteLink(anyString(), anyString(), any()))
                .thenReturn(Mono.just(actionLink));

        Invite result = organizationDomainService.createInvite(orgId, email, userRole, "1", invitedBy);

        assertThat(result.email()).isEqualTo(email);
        assertThat(result.userRole()).isEqualTo(userRole);
        assertThat(result.token()).isEqualTo(token);
        assertThat(result.status()).isEqualTo("pending");
    }

    @Test
    void createInvite_sendsInviteEmailWithOrgName() {
        when(supabaseService.post(eq("admin.invites"), any(), eq(InviteResponse[].class)))
                .thenReturn(new InviteResponse[]{inviteResponse()});
        when(supabaseService.getListWithQuery(eq("admin.organizations"), anyString(), eq(OrganizationResponse.class)))
                .thenReturn(List.of(orgResponse("Acme Corp")));
        when(supabaseAdminClient.generateInviteLink(anyString(), anyString(), any()))
                .thenReturn(Mono.just(actionLink));

        organizationDomainService.createInvite(orgId, email, userRole, "1", invitedBy);

        verify(inviteMailService).sendInvite(email, "Acme Corp", userRole, actionLink);
    }

    @Test
    void createInvite_whenOrgNotFound_usesDefaultOrgName() {
        when(supabaseService.post(eq("admin.invites"), any(), eq(InviteResponse[].class)))
                .thenReturn(new InviteResponse[]{inviteResponse()});
        when(supabaseService.getListWithQuery(eq("admin.organizations"), anyString(), eq(OrganizationResponse.class)))
                .thenReturn(List.of());
        when(supabaseAdminClient.generateInviteLink(anyString(), anyString(), any()))
                .thenReturn(Mono.just(actionLink));

        organizationDomainService.createInvite(orgId, email, userRole, "1", invitedBy);

        verify(inviteMailService).sendInvite(eq(email), eq("the organization"), eq(userRole), anyString());
    }

    @Test
    void createInvite_generatesRedirectToWithInviteToken() {
        when(supabaseService.post(eq("admin.invites"), any(), eq(InviteResponse[].class)))
                .thenReturn(new InviteResponse[]{inviteResponse()});
        when(supabaseService.getListWithQuery(eq("admin.organizations"), anyString(), eq(OrganizationResponse.class)))
                .thenReturn(List.of(orgResponse("Acme")));
        when(supabaseAdminClient.generateInviteLink(anyString(), anyString(), any()))
                .thenReturn(Mono.just(actionLink));

        organizationDomainService.createInvite(orgId, email, userRole, "1", invitedBy);

        verify(supabaseAdminClient).generateInviteLink(
                eq(email),
                contains("http://localhost:4300/onboarding?invite_token=" + token),
                any()
        );
    }

    @Test
    void createInvite_whenSupabaseReturnsNull_throwsEntityNotCreatedException() {
        when(supabaseService.post(eq("admin.invites"), any(), eq(InviteResponse[].class)))
                .thenReturn(null);

        assertThatThrownBy(() -> organizationDomainService.createInvite(orgId, email, userRole, "1", invitedBy))
                .isInstanceOf(EntityNotCreatedException.class);
    }

    @Test
    void createInvite_whenSupabaseReturnsEmptyArray_throwsEntityNotCreatedException() {
        when(supabaseService.post(eq("admin.invites"), any(), eq(InviteResponse[].class)))
                .thenReturn(new InviteResponse[]{});

        assertThatThrownBy(() -> organizationDomainService.createInvite(orgId, email, userRole, "1", invitedBy))
                .isInstanceOf(EntityNotCreatedException.class);
    }

    // --- listInvites ---

    @Test
    void listInvites_returnsMappedList() {
        var invite1 = inviteResponse();
        var invite2 = new InviteResponse(UUID.randomUUID(), orgId, "other@example.com", "admin",
                UUID.randomUUID(), "pending", invitedBy, OffsetDateTime.now(), OffsetDateTime.now().plusDays(7), null);
        when(supabaseService.getListWithQuery(eq("admin.invites"), anyString(), eq(InviteResponse.class)))
                .thenReturn(List.of(invite1, invite2));

        List<Invite> result = organizationDomainService.listInvites(orgId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).email()).isEqualTo(email);
        assertThat(result.get(1).email()).isEqualTo("other@example.com");
    }

    @Test
    void listInvites_whenEmpty_returnsEmptyList() {
        when(supabaseService.getListWithQuery(eq("admin.invites"), anyString(), eq(InviteResponse.class)))
                .thenReturn(List.of());

        assertThat(organizationDomainService.listInvites(orgId)).isEmpty();
    }

    // --- deleteInvite ---

    @Test
    void deleteInvite_whenOneDeleted_doesNotThrow() {
        when(supabaseService.deleteReturningCount(eq("admin.invites"), anyString())).thenReturn(1);

        organizationDomainService.deleteInvite(orgId, inviteId);

        verify(supabaseService).deleteReturningCount(eq("admin.invites"), contains(inviteId.toString()));
    }

    @Test
    void deleteInvite_whenNoneDeleted_throwsNoRowsAffectedException() {
        when(supabaseService.deleteReturningCount(eq("admin.invites"), anyString())).thenReturn(0);

        assertThatThrownBy(() -> organizationDomainService.deleteInvite(orgId, inviteId))
                .isInstanceOf(NoRowsAffectedException.class);
    }

    // --- deleteOrganization ---

    @Test
    void deleteOrganization_whenOneDeleted_doesNotThrow() {
        UUID id = UUID.randomUUID();
        when(supabaseService.deleteReturningCount(eq("admin.organizations"), anyString())).thenReturn(1);

        organizationDomainService.deleteOrganization(id);

        verify(supabaseService).deleteReturningCount(eq("admin.organizations"), contains(id.toString()));
    }

    @Test
    void deleteOrganization_whenNoneDeleted_throwsNoRowsAffectedException() {
        when(supabaseService.deleteReturningCount(eq("admin.organizations"), anyString())).thenReturn(0);

        assertThatThrownBy(() -> organizationDomainService.deleteOrganization(UUID.randomUUID()))
                .isInstanceOf(NoRowsAffectedException.class);
    }

    // --- createOrganization ---

    @Test
    void createOrganization_returnsCreatedOrganization() {
        when(supabaseService.post(eq("admin.organizations"), any(), eq(OrganizationResponse[].class)))
                .thenReturn(new OrganizationResponse[]{orgResponse("New Org")});

        Organization result = organizationDomainService.createOrganization("New Org");

        assertThat(result.id()).isEqualTo(orgId);
        assertThat(result.name()).isEqualTo("New Org");
    }

    @Test
    void createOrganization_whenSupabaseReturnsEmpty_throwsEntityNotCreatedException() {
        when(supabaseService.post(eq("admin.organizations"), any(), eq(OrganizationResponse[].class)))
                .thenReturn(new OrganizationResponse[]{});

        assertThatThrownBy(() -> organizationDomainService.createOrganization("New Org"))
                .isInstanceOf(EntityNotCreatedException.class);
    }

    // --- helpers ---

    private InviteResponse inviteResponse() {
        return new InviteResponse(inviteId, orgId, email, userRole, token, "pending",
                invitedBy, OffsetDateTime.now(), OffsetDateTime.now().plusDays(7), null);
    }

    private OrganizationResponse orgResponse(String name) {
        return new OrganizationResponse(orgId, name, memberId,OffsetDateTime.now(), OffsetDateTime.now());
    }

    // =====================================================================
    // ReminderService Tests
    // =====================================================================

    @Nested
    class ReminderServiceTests {

        @Mock
        private ReminderMailService reminderMailService;

        @Mock
        private ReminderSmsService reminderSmsService;

        private ReminderService reminderService;

        private final UUID remOrgId = UUID.randomUUID();
        private final UUID userId1 = UUID.randomUUID();
        private final UUID userId2 = UUID.randomUUID();
        private final String remOrgName = "Acme Corp";
        private final String remAppUrl = "https://app.time2log.ch";

        // Use the same timezone as ReminderService so tests pass on UTC CI servers
        private static final java.time.ZoneId SWISS_ZONE = java.time.ZoneId.of("Europe/Zurich");
        private final String currentDayName = LocalDate.now(SWISS_ZONE).getDayOfWeek().name();
        private final String currentTime = LocalTime.now(SWISS_ZONE).withSecond(0).withNano(0).toString();

        @BeforeEach
        void setUpReminder() {
            reminderService = new ReminderService(supabaseAdminClient, reminderMailService, reminderSmsService);
            ReflectionTestUtils.setField(reminderService, "appUrl", remAppUrl);
        }

        // --- Reminder config matching ---

        @Test
        void sendWeeklyReminders_noReminderConfigs_doesNothing() {
            when(supabaseAdminClient.getListWithQuery(eq("admin.reminder"), anyString(), eq(ReminderResponse.class)))
                    .thenReturn(List.of());

            reminderService.sendWeeklyReminders();

            verifyNoInteractions(reminderMailService);
        }

        @Test
        void sendWeeklyReminders_reminderOnDifferentDay_doesNothing() {
            var otherDay = LocalDate.now(SWISS_ZONE).getDayOfWeek() == java.time.DayOfWeek.MONDAY ? "TUESDAY" : "MONDAY";
            var reminder = reminderConfig(remOrgId, "EMAIL", currentTime, 3, otherDay);

            when(supabaseAdminClient.getListWithQuery(eq("admin.reminder"), anyString(), eq(ReminderResponse.class)))
                    .thenReturn(List.of(reminder));

            reminderService.sendWeeklyReminders();

            verifyNoInteractions(reminderMailService);
        }

        @Test
        void sendWeeklyReminders_reminderOnDifferentTime_doesNothing() {
            var otherTime = LocalTime.now(SWISS_ZONE).plusHours(2).withSecond(0).withNano(0).toString();
            var reminder = reminderConfig(remOrgId, "EMAIL", otherTime, 3, currentDayName);

            when(supabaseAdminClient.getListWithQuery(eq("admin.reminder"), anyString(), eq(ReminderResponse.class)))
                    .thenReturn(List.of(reminder));

            reminderService.sendWeeklyReminders();

            verifyNoInteractions(reminderMailService);
        }

        // --- EMAIL channel: full flow ---

        @Test
        void sendWeeklyReminders_inactiveUser_sendsEmailReminder() {
            var idleDays = 5;
            stubReminderWithOrg("EMAIL", idleDays);
            stubMember(userId1);
            stubInactiveUser(userId1, 7);
            stubProfile(userId1, "Max");
            when(supabaseAdminClient.getUserEmail(userId1)).thenReturn("max@example.com");

            reminderService.sendWeeklyReminders();

            verify(reminderMailService).sendReminder("max@example.com", "Max", remOrgName, 7, remAppUrl);
        }

        @Test
        void sendWeeklyReminders_activeUser_doesNotSendReminder() {
            stubReminderWithOrg("EMAIL", 3);
            stubMember(userId1);
            var recentRecord = activityRecordResponse(userId1, remOrgId, LocalDate.now(SWISS_ZONE).toString());
            when(supabaseAdminClient.getListWithQuery(eq("app.activity_records"), contains("entry_date=gte."), eq(ActivityRecordResponse.class)))
                    .thenReturn(List.of(recentRecord));

            reminderService.sendWeeklyReminders();

            verifyNoInteractions(reminderMailService);
        }

        @Test
        void sendWeeklyReminders_userWithNoRecordsAtAll_usesConfiguredIdleDays() {
            var idleDays = 7;
            stubReminderWithOrg("EMAIL", idleDays);
            stubMember(userId1);
            // No recent records and no records at all
            when(supabaseAdminClient.getListWithQuery(eq("app.activity_records"), contains("entry_date=gte."), eq(ActivityRecordResponse.class)))
                    .thenReturn(List.of());
            when(supabaseAdminClient.getListWithQuery(eq("app.activity_records"), contains("order=entry_date.desc"), eq(ActivityRecordResponse.class)))
                    .thenReturn(List.of());
            stubProfile(userId1, "Anna");
            when(supabaseAdminClient.getUserEmail(userId1)).thenReturn("anna@example.com");

            reminderService.sendWeeklyReminders();

            verify(reminderMailService).sendReminder("anna@example.com", "Anna", remOrgName, 7, remAppUrl);
        }

        @Test
        void sendWeeklyReminders_profileNotFound_usesFallbackName() {
            stubReminderWithOrg("EMAIL", 3);
            stubMember(userId1);
            stubInactiveUserNoRecords(userId1);
            when(supabaseAdminClient.getListWithQuery(eq("app.profiles"), anyString(), eq(ProfileResponse.class)))
                    .thenReturn(List.of());
            when(supabaseAdminClient.getUserEmail(userId1)).thenReturn("unknown@example.com");

            reminderService.sendWeeklyReminders();

            verify(reminderMailService).sendReminder("unknown@example.com", "User", remOrgName, 3, remAppUrl);
        }

        @Test
        void sendWeeklyReminders_emailIsNull_doesNotSendReminder() {
            stubReminderWithOrg("EMAIL", 3);
            stubMember(userId1);
            stubInactiveUserNoRecords(userId1);
            stubProfile(userId1, "Ghost");
            when(supabaseAdminClient.getUserEmail(userId1)).thenReturn(null);

            reminderService.sendWeeklyReminders();

            verifyNoInteractions(reminderMailService);
        }

        @Test
        void sendWeeklyReminders_emailIsBlank_doesNotSendReminder() {
            stubReminderWithOrg("EMAIL", 3);
            stubMember(userId1);
            stubInactiveUserNoRecords(userId1);
            stubProfile(userId1, "Ghost");
            when(supabaseAdminClient.getUserEmail(userId1)).thenReturn("   ");

            reminderService.sendWeeklyReminders();

            verifyNoInteractions(reminderMailService);
        }

        @Test
        void sendWeeklyReminders_noMembers_doesNotSendReminder() {
            stubReminderWithOrg("EMAIL", 3);
            when(supabaseAdminClient.getListWithQuery(eq("admin.organization_members"), anyString(), eq(OrganizationMemberResponse.class)))
                    .thenReturn(List.of());

            reminderService.sendWeeklyReminders();

            verifyNoInteractions(reminderMailService);
        }

        @Test
        void sendWeeklyReminders_orgNotFound_doesNotSendReminder() {
            var reminder = reminderConfig(remOrgId, "EMAIL", currentTime, 3, currentDayName);
            when(supabaseAdminClient.getListWithQuery(eq("admin.reminder"), anyString(), eq(ReminderResponse.class)))
                    .thenReturn(List.of(reminder));
            when(supabaseAdminClient.getListWithQuery(eq("admin.organizations"), contains("id=eq." + remOrgId), eq(OrganizationResponse.class)))
                    .thenReturn(List.of());

            reminderService.sendWeeklyReminders();

            verifyNoInteractions(reminderMailService);
        }

        // --- SMS channel ---

        @Test
        void sendWeeklyReminders_smsChannel_sendsSmsReminder() {
            stubReminderWithOrg("SMS", 3);
            stubMember(userId1);
            stubInactiveUserNoRecords(userId1);
            stubProfile(userId1, "Max");

            reminderService.sendWeeklyReminders();

            verify(reminderSmsService).sendReminder("+41791234567", "Max", remOrgName, 3);
            verifyNoInteractions(reminderMailService);
        }

        @Test
        void sendWeeklyReminders_smsChannel_noPhoneNumber_skips() {
            stubReminderWithOrg("SMS", 3);
            stubMember(userId1);
            stubInactiveUserNoRecords(userId1);
            when(supabaseAdminClient.getListWithQuery(eq("app.profiles"), contains("id=eq." + userId1), eq(ProfileResponse.class)))
                    .thenReturn(List.of(profileResponseNoPhone(userId1, "Max")));

            reminderService.sendWeeklyReminders();

            verifyNoInteractions(reminderSmsService);
            verifyNoInteractions(reminderMailService);
        }

        @Test
        void sendWeeklyReminders_smsChannel_noProfile_skips() {
            stubReminderWithOrg("SMS", 3);
            stubMember(userId1);
            stubInactiveUserNoRecords(userId1);
            when(supabaseAdminClient.getListWithQuery(eq("app.profiles"), contains("id=eq." + userId1), eq(ProfileResponse.class)))
                    .thenReturn(List.of());

            reminderService.sendWeeklyReminders();

            verifyNoInteractions(reminderSmsService);
            verifyNoInteractions(reminderMailService);
        }

        // --- Multiple orgs ---

        @Test
        void sendWeeklyReminders_multipleReminders_sendsToMatchingOrgsOnly() {
            var orgId2 = UUID.randomUUID();
            var otherDay = LocalDate.now(SWISS_ZONE).getDayOfWeek() == java.time.DayOfWeek.MONDAY ? "TUESDAY" : "MONDAY";

            // One reminder matches today, one does not
            var matchingReminder = reminderConfig(remOrgId, "EMAIL", currentTime, 3, currentDayName);
            var nonMatchingReminder = reminderConfig(orgId2, "EMAIL", currentTime, 3, otherDay);

            when(supabaseAdminClient.getListWithQuery(eq("admin.reminder"), anyString(), eq(ReminderResponse.class)))
                    .thenReturn(List.of(matchingReminder, nonMatchingReminder));
            when(supabaseAdminClient.getListWithQuery(eq("admin.organizations"), contains("id=eq." + remOrgId), eq(OrganizationResponse.class)))
                    .thenReturn(List.of(reminderOrgResponse(remOrgId, remOrgName)));
            stubMember(userId1);
            stubInactiveUserNoRecords(userId1);
            stubProfile(userId1, "Max");
            when(supabaseAdminClient.getUserEmail(userId1)).thenReturn("max@example.com");

            reminderService.sendWeeklyReminders();

            verify(reminderMailService).sendReminder("max@example.com", "Max", remOrgName, 3, remAppUrl);
            verifyNoMoreInteractions(reminderMailService);
        }

        // --- Error handling ---

        @Test
        void sendWeeklyReminders_errorForOneUser_continuesWithNext() {
            stubReminderWithOrg("EMAIL", 3);
            when(supabaseAdminClient.getListWithQuery(eq("admin.organization_members"), anyString(), eq(OrganizationMemberResponse.class)))
                    .thenReturn(List.of(memberResponse(userId1, remOrgId), memberResponse(userId2, remOrgId)));
            when(supabaseAdminClient.getListWithQuery(eq("app.activity_records"), contains("entry_date=gte."), eq(ActivityRecordResponse.class)))
                    .thenReturn(List.of());
            when(supabaseAdminClient.getListWithQuery(eq("app.activity_records"), contains("order=entry_date.desc"), eq(ActivityRecordResponse.class)))
                    .thenReturn(List.of());
            when(supabaseAdminClient.getListWithQuery(eq("app.profiles"), contains("id=eq." + userId1), eq(ProfileResponse.class)))
                    .thenReturn(List.of(profileResponse(userId1, "Fail")));
            when(supabaseAdminClient.getListWithQuery(eq("app.profiles"), contains("id=eq." + userId2), eq(ProfileResponse.class)))
                    .thenReturn(List.of(profileResponse(userId2, "Success")));
            when(supabaseAdminClient.getUserEmail(userId1)).thenThrow(new SupabaseApiException(404, "User not found"));
            when(supabaseAdminClient.getUserEmail(userId2)).thenReturn("success@example.com");

            reminderService.sendWeeklyReminders();

            verify(reminderMailService, never()).sendReminder(anyString(), eq("Fail"), anyString(), anyLong(), anyString());
            verify(reminderMailService).sendReminder("success@example.com", "Success", remOrgName, 3, remAppUrl);
        }

        @Test
        void sendWeeklyReminders_correctDaysInactiveCalculation() {
            stubReminderWithOrg("EMAIL", 3);
            stubMember(userId1);
            stubInactiveUser(userId1, 10);
            stubProfile(userId1, "Test");
            when(supabaseAdminClient.getUserEmail(userId1)).thenReturn("test@example.com");

            reminderService.sendWeeklyReminders();

            verify(reminderMailService).sendReminder(eq("test@example.com"), eq("Test"), eq(remOrgName), eq(10L), eq(remAppUrl));
        }

        // --- OrganizationDomainService: getReminder / saveReminder ---

        @Test
        void getReminder_returnsExistingReminder() {
            var response = reminderConfig(orgId, "EMAIL", "08:00:00", 3, "FRIDAY");
            when(supabaseService.getListWithQuery(eq("admin.reminder"), contains("organization_id=eq." + orgId), eq(ReminderResponse.class)))
                    .thenReturn(List.of(response));

            var result = organizationDomainService.getReminder(orgId);

            assertThat(result).isNotNull();
            assertThat(result.channel()).isEqualTo("EMAIL");
            assertThat(result.idle_days()).isEqualTo(3);
            assertThat(result.send_day()).isEqualTo("FRIDAY");
        }

        @Test
        void getReminder_returnsNullWhenNoneExists() {
            when(supabaseService.getListWithQuery(eq("admin.reminder"), contains("organization_id=eq." + orgId), eq(ReminderResponse.class)))
                    .thenReturn(List.of());

            var result = organizationDomainService.getReminder(orgId);

            assertThat(result).isNull();
        }

        @Test
        void saveReminder_createsNewWhenNoneExists() {
            when(supabaseService.getListWithQuery(eq("admin.reminder"), anyString(), eq(ReminderResponse.class)))
                    .thenReturn(List.of());
            var created = reminderConfig(orgId, "SMS", "09:00:00", 5, "MONDAY");
            when(supabaseService.post(eq("admin.reminder"), any(), eq(ReminderResponse[].class)))
                    .thenReturn(new ReminderResponse[]{created});

            var result = organizationDomainService.saveReminder(orgId, "SMS", "09:00:00", 5, "MONDAY");

            assertThat(result.channel()).isEqualTo("SMS");
            assertThat(result.idle_days()).isEqualTo(5);
            verify(supabaseService).post(eq("admin.reminder"), any(), eq(ReminderResponse[].class));
        }

        @Test
        void saveReminder_updatesExistingReminder() {
            var existing = reminderConfig(orgId, "EMAIL", "08:00:00", 3, "FRIDAY");
            when(supabaseService.getListWithQuery(eq("admin.reminder"), anyString(), eq(ReminderResponse.class)))
                    .thenReturn(List.of(existing));
            var updated = reminderConfig(orgId, "SMS", "10:00:00", 7, "WEDNESDAY");
            when(supabaseService.patch(eq("admin.reminder"), contains("organization_id=eq." + orgId), any(), eq(ReminderResponse[].class)))
                    .thenReturn(new ReminderResponse[]{updated});

            var result = organizationDomainService.saveReminder(orgId, "SMS", "10:00:00", 7, "WEDNESDAY");

            assertThat(result.channel()).isEqualTo("SMS");
            assertThat(result.idle_days()).isEqualTo(7);
            assertThat(result.send_day()).isEqualTo("WEDNESDAY");
            verify(supabaseService).patch(eq("admin.reminder"), anyString(), any(), eq(ReminderResponse[].class));
        }

        @Test
        void saveReminder_throwsWhenCreateFails() {
            when(supabaseService.getListWithQuery(eq("admin.reminder"), anyString(), eq(ReminderResponse.class)))
                    .thenReturn(List.of());
            when(supabaseService.post(eq("admin.reminder"), any(), eq(ReminderResponse[].class)))
                    .thenReturn(new ReminderResponse[]{});

            assertThatThrownBy(() -> organizationDomainService.saveReminder(orgId, "EMAIL", "08:00:00", 3, "FRIDAY"))
                    .isInstanceOf(EntityNotCreatedException.class);
        }

        @Test
        void saveReminder_throwsWhenUpdateFails() {
            var existing = reminderConfig(orgId, "EMAIL", "08:00:00", 3, "FRIDAY");
            when(supabaseService.getListWithQuery(eq("admin.reminder"), anyString(), eq(ReminderResponse.class)))
                    .thenReturn(List.of(existing));
            when(supabaseService.patch(eq("admin.reminder"), anyString(), any(), eq(ReminderResponse[].class)))
                    .thenReturn(new ReminderResponse[]{});

            assertThatThrownBy(() -> organizationDomainService.saveReminder(orgId, "SMS", "10:00:00", 7, "MONDAY"))
                    .isInstanceOf(EntityNotCreatedException.class);
        }

        // --- Shared stubs ---

        private void stubReminderWithOrg(String channel, int idleDays) {
            var reminder = reminderConfig(remOrgId, channel, currentTime, idleDays, currentDayName);
            when(supabaseAdminClient.getListWithQuery(eq("admin.reminder"), anyString(), eq(ReminderResponse.class)))
                    .thenReturn(List.of(reminder));
            when(supabaseAdminClient.getListWithQuery(eq("admin.organizations"), contains("id=eq." + remOrgId), eq(OrganizationResponse.class)))
                    .thenReturn(List.of(reminderOrgResponse(remOrgId, remOrgName)));
        }

        private void stubMember(UUID userId) {
            when(supabaseAdminClient.getListWithQuery(eq("admin.organization_members"), anyString(), eq(OrganizationMemberResponse.class)))
                    .thenReturn(List.of(memberResponse(userId, remOrgId)));
        }

        private void stubInactiveUserNoRecords(UUID userId) {
            when(supabaseAdminClient.getListWithQuery(eq("app.activity_records"), contains("entry_date=gte."), eq(ActivityRecordResponse.class)))
                    .thenReturn(List.of());
            when(supabaseAdminClient.getListWithQuery(eq("app.activity_records"), contains("order=entry_date.desc"), eq(ActivityRecordResponse.class)))
                    .thenReturn(List.of());
        }

        private void stubInactiveUser(UUID userId, int daysAgo) {
            when(supabaseAdminClient.getListWithQuery(eq("app.activity_records"), contains("entry_date=gte."), eq(ActivityRecordResponse.class)))
                    .thenReturn(List.of());
            var lastRecord = activityRecordResponse(userId, remOrgId, LocalDate.now(SWISS_ZONE).minusDays(daysAgo).toString());
            when(supabaseAdminClient.getListWithQuery(eq("app.activity_records"), contains("order=entry_date.desc"), eq(ActivityRecordResponse.class)))
                    .thenReturn(List.of(lastRecord));
        }

        private void stubProfile(UUID userId, String firstName) {
            when(supabaseAdminClient.getListWithQuery(eq("app.profiles"), contains("id=eq." + userId), eq(ProfileResponse.class)))
                    .thenReturn(List.of(profileResponse(userId, firstName)));
        }

        // --- Helpers ---

        private ReminderResponse reminderConfig(UUID orgId, String channel, String sendTime, int idleDays, String sendDay) {
            return new ReminderResponse(UUID.randomUUID(), orgId, channel, sendTime, idleDays, sendDay);
        }

        private OrganizationResponse reminderOrgResponse(UUID id, String name) {
            return new OrganizationResponse(id, name, UUID.randomUUID(), OffsetDateTime.now(), OffsetDateTime.now());
        }

        private OrganizationMemberResponse memberResponse(UUID userId, UUID orgId) {
            return new OrganizationMemberResponse(userId, orgId, "user", OffsetDateTime.now());
        }

        private ProfileResponse profileResponse(UUID id, String firstName) {
            return new ProfileResponse(id, firstName, "Lastname", "+41791234567", OffsetDateTime.now(), OffsetDateTime.now());
        }

        private ProfileResponse profileResponseNoPhone(UUID id, String firstName) {
            return new ProfileResponse(id, firstName, "Lastname", null, OffsetDateTime.now(), OffsetDateTime.now());
        }

        private ActivityRecordResponse activityRecordResponse(UUID userId, UUID orgId, String entryDate) {
            return new ActivityRecordResponse(
                    UUID.randomUUID(), orgId, userId, UUID.randomUUID(), UUID.randomUUID(),
                    entryDate, 8, "notes", 3, "office", "HS24",
                    OffsetDateTime.now(), OffsetDateTime.now()
            );
        }
    }
}
