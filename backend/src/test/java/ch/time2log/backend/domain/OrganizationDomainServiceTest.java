package ch.time2log.backend.domain;

import ch.time2log.backend.domain.exception.EntityNotCreatedException;
import ch.time2log.backend.domain.exception.NoRowsAffectedException;
import ch.time2log.backend.domain.models.Invite;
import ch.time2log.backend.domain.models.Organization;
import ch.time2log.backend.infrastructure.mail.InviteMailService;
import ch.time2log.backend.infrastructure.mail.ReminderMailService;
import ch.time2log.backend.infrastructure.supabase.SupabaseAdminClient;
import ch.time2log.backend.infrastructure.supabase.SupabaseApiException;
import ch.time2log.backend.infrastructure.supabase.SupabaseService;
import ch.time2log.backend.infrastructure.supabase.responses.ActivityRecordResponse;
import ch.time2log.backend.infrastructure.supabase.responses.InviteResponse;
import ch.time2log.backend.infrastructure.supabase.responses.OrganizationMemberResponse;
import ch.time2log.backend.infrastructure.supabase.responses.OrganizationResponse;
import ch.time2log.backend.infrastructure.supabase.responses.ProfileResponse;
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
        return new OrganizationResponse(orgId, name, OffsetDateTime.now(), OffsetDateTime.now());
    }

    // =====================================================================
    // ReminderService Tests
    // =====================================================================

    @Nested
    class ReminderServiceTests {

        @Mock
        private ReminderMailService reminderMailService;

        private ReminderService reminderService;

        private final UUID remOrgId = UUID.randomUUID();
        private final UUID userId1 = UUID.randomUUID();
        private final UUID userId2 = UUID.randomUUID();
        private final String remOrgName = "Acme Corp";
        private final String remAppUrl = "https://dev.app.aeristo.cc/";

        @BeforeEach
        void setUpReminder() {
            reminderService = new ReminderService(supabaseAdminClient, reminderMailService);
            ReflectionTestUtils.setField(reminderService, "appUrl", remAppUrl);
        }

        @Test
        void sendWeeklyReminders_inactiveUser_sendsReminderEmail() {
            var cutoffDate = LocalDate.now().minusDays(3).toString();

            when(supabaseAdminClient.getListWithQuery(eq("admin.organizations"), anyString(), eq(OrganizationResponse.class)))
                    .thenReturn(List.of(reminderOrgResponse(remOrgId, remOrgName)));
            when(supabaseAdminClient.getListWithQuery(eq("admin.organization_members"), contains("organization_id=eq." + remOrgId), eq(OrganizationMemberResponse.class)))
                    .thenReturn(List.of(memberResponse(userId1, remOrgId)));
            when(supabaseAdminClient.getListWithQuery(eq("app.activity_records"), contains("entry_date=gte." + cutoffDate), eq(ActivityRecordResponse.class)))
                    .thenReturn(List.of());
            var lastRecord = activityRecordResponse(userId1, remOrgId, LocalDate.now().minusDays(5).toString());
            when(supabaseAdminClient.getListWithQuery(eq("app.activity_records"), contains("order=entry_date.desc"), eq(ActivityRecordResponse.class)))
                    .thenReturn(List.of(lastRecord));
            when(supabaseAdminClient.getListWithQuery(eq("app.profiles"), contains("id=eq." + userId1), eq(ProfileResponse.class)))
                    .thenReturn(List.of(profileResponse(userId1, "Max")));
            when(supabaseAdminClient.getUserEmail(userId1)).thenReturn("max@example.com");

            reminderService.sendWeeklyReminders();

            verify(reminderMailService).sendReminder("max@example.com", "Max", remOrgName, 5, remAppUrl);
        }

        @Test
        void sendWeeklyReminders_activeUser_doesNotSendReminder() {
            var recentRecord = activityRecordResponse(userId1, remOrgId, LocalDate.now().toString());

            when(supabaseAdminClient.getListWithQuery(eq("admin.organizations"), anyString(), eq(OrganizationResponse.class)))
                    .thenReturn(List.of(reminderOrgResponse(remOrgId, remOrgName)));
            when(supabaseAdminClient.getListWithQuery(eq("admin.organization_members"), anyString(), eq(OrganizationMemberResponse.class)))
                    .thenReturn(List.of(memberResponse(userId1, remOrgId)));
            when(supabaseAdminClient.getListWithQuery(eq("app.activity_records"), contains("entry_date=gte."), eq(ActivityRecordResponse.class)))
                    .thenReturn(List.of(recentRecord));

            reminderService.sendWeeklyReminders();

            verifyNoInteractions(reminderMailService);
        }

        @Test
        void sendWeeklyReminders_userWithNoRecordsAtAll_usesDefaultDaysInactive() {
            when(supabaseAdminClient.getListWithQuery(eq("admin.organizations"), anyString(), eq(OrganizationResponse.class)))
                    .thenReturn(List.of(reminderOrgResponse(remOrgId, remOrgName)));
            when(supabaseAdminClient.getListWithQuery(eq("admin.organization_members"), anyString(), eq(OrganizationMemberResponse.class)))
                    .thenReturn(List.of(memberResponse(userId1, remOrgId)));
            when(supabaseAdminClient.getListWithQuery(eq("app.activity_records"), contains("entry_date=gte."), eq(ActivityRecordResponse.class)))
                    .thenReturn(List.of());
            when(supabaseAdminClient.getListWithQuery(eq("app.activity_records"), contains("order=entry_date.desc"), eq(ActivityRecordResponse.class)))
                    .thenReturn(List.of());
            when(supabaseAdminClient.getListWithQuery(eq("app.profiles"), contains("id=eq." + userId1), eq(ProfileResponse.class)))
                    .thenReturn(List.of(profileResponse(userId1, "Anna")));
            when(supabaseAdminClient.getUserEmail(userId1)).thenReturn("anna@example.com");

            reminderService.sendWeeklyReminders();

            verify(reminderMailService).sendReminder("anna@example.com", "Anna", remOrgName, 3, remAppUrl);
        }

        @Test
        void sendWeeklyReminders_profileNotFound_usesFallbackName() {
            when(supabaseAdminClient.getListWithQuery(eq("admin.organizations"), anyString(), eq(OrganizationResponse.class)))
                    .thenReturn(List.of(reminderOrgResponse(remOrgId, remOrgName)));
            when(supabaseAdminClient.getListWithQuery(eq("admin.organization_members"), anyString(), eq(OrganizationMemberResponse.class)))
                    .thenReturn(List.of(memberResponse(userId1, remOrgId)));
            when(supabaseAdminClient.getListWithQuery(eq("app.activity_records"), contains("entry_date=gte."), eq(ActivityRecordResponse.class)))
                    .thenReturn(List.of());
            when(supabaseAdminClient.getListWithQuery(eq("app.activity_records"), contains("order=entry_date.desc"), eq(ActivityRecordResponse.class)))
                    .thenReturn(List.of());
            when(supabaseAdminClient.getListWithQuery(eq("app.profiles"), anyString(), eq(ProfileResponse.class)))
                    .thenReturn(List.of());
            when(supabaseAdminClient.getUserEmail(userId1)).thenReturn("unknown@example.com");

            reminderService.sendWeeklyReminders();

            verify(reminderMailService).sendReminder("unknown@example.com", "User", remOrgName, 3, remAppUrl);
        }

        @Test
        void sendWeeklyReminders_emailIsNull_doesNotSendReminder() {
            when(supabaseAdminClient.getListWithQuery(eq("admin.organizations"), anyString(), eq(OrganizationResponse.class)))
                    .thenReturn(List.of(reminderOrgResponse(remOrgId, remOrgName)));
            when(supabaseAdminClient.getListWithQuery(eq("admin.organization_members"), anyString(), eq(OrganizationMemberResponse.class)))
                    .thenReturn(List.of(memberResponse(userId1, remOrgId)));
            when(supabaseAdminClient.getListWithQuery(eq("app.activity_records"), contains("entry_date=gte."), eq(ActivityRecordResponse.class)))
                    .thenReturn(List.of());
            when(supabaseAdminClient.getListWithQuery(eq("app.activity_records"), contains("order=entry_date.desc"), eq(ActivityRecordResponse.class)))
                    .thenReturn(List.of());
            when(supabaseAdminClient.getListWithQuery(eq("app.profiles"), anyString(), eq(ProfileResponse.class)))
                    .thenReturn(List.of(profileResponse(userId1, "Ghost")));
            when(supabaseAdminClient.getUserEmail(userId1)).thenReturn(null);

            reminderService.sendWeeklyReminders();

            verifyNoInteractions(reminderMailService);
        }

        @Test
        void sendWeeklyReminders_emailIsBlank_doesNotSendReminder() {
            when(supabaseAdminClient.getListWithQuery(eq("admin.organizations"), anyString(), eq(OrganizationResponse.class)))
                    .thenReturn(List.of(reminderOrgResponse(remOrgId, remOrgName)));
            when(supabaseAdminClient.getListWithQuery(eq("admin.organization_members"), anyString(), eq(OrganizationMemberResponse.class)))
                    .thenReturn(List.of(memberResponse(userId1, remOrgId)));
            when(supabaseAdminClient.getListWithQuery(eq("app.activity_records"), contains("entry_date=gte."), eq(ActivityRecordResponse.class)))
                    .thenReturn(List.of());
            when(supabaseAdminClient.getListWithQuery(eq("app.activity_records"), contains("order=entry_date.desc"), eq(ActivityRecordResponse.class)))
                    .thenReturn(List.of());
            when(supabaseAdminClient.getListWithQuery(eq("app.profiles"), anyString(), eq(ProfileResponse.class)))
                    .thenReturn(List.of(profileResponse(userId1, "Ghost")));
            when(supabaseAdminClient.getUserEmail(userId1)).thenReturn("   ");

            reminderService.sendWeeklyReminders();

            verifyNoInteractions(reminderMailService);
        }

        @Test
        void sendWeeklyReminders_noOrganizations_doesNothing() {
            when(supabaseAdminClient.getListWithQuery(eq("admin.organizations"), anyString(), eq(OrganizationResponse.class)))
                    .thenReturn(List.of());

            reminderService.sendWeeklyReminders();

            verifyNoInteractions(reminderMailService);
        }

        @Test
        void sendWeeklyReminders_noMembers_doesNotSendReminder() {
            when(supabaseAdminClient.getListWithQuery(eq("admin.organizations"), anyString(), eq(OrganizationResponse.class)))
                    .thenReturn(List.of(reminderOrgResponse(remOrgId, remOrgName)));
            when(supabaseAdminClient.getListWithQuery(eq("admin.organization_members"), anyString(), eq(OrganizationMemberResponse.class)))
                    .thenReturn(List.of());

            reminderService.sendWeeklyReminders();

            verifyNoInteractions(reminderMailService);
        }

        @Test
        void sendWeeklyReminders_multipleOrgsAndUsers_sendsCorrectReminders() {
            var orgId2 = UUID.randomUUID();

            when(supabaseAdminClient.getListWithQuery(eq("admin.organizations"), anyString(), eq(OrganizationResponse.class)))
                    .thenReturn(List.of(reminderOrgResponse(remOrgId, "Org A"), reminderOrgResponse(orgId2, "Org B")));
            when(supabaseAdminClient.getListWithQuery(eq("admin.organization_members"), contains("organization_id=eq." + remOrgId), eq(OrganizationMemberResponse.class)))
                    .thenReturn(List.of(memberResponse(userId1, remOrgId)));
            when(supabaseAdminClient.getListWithQuery(eq("admin.organization_members"), contains("organization_id=eq." + orgId2), eq(OrganizationMemberResponse.class)))
                    .thenReturn(List.of(memberResponse(userId2, orgId2)));
            when(supabaseAdminClient.getListWithQuery(eq("app.activity_records"), contains("entry_date=gte."), eq(ActivityRecordResponse.class)))
                    .thenReturn(List.of());
            when(supabaseAdminClient.getListWithQuery(eq("app.activity_records"), contains("order=entry_date.desc"), eq(ActivityRecordResponse.class)))
                    .thenReturn(List.of());
            when(supabaseAdminClient.getListWithQuery(eq("app.profiles"), contains("id=eq." + userId1), eq(ProfileResponse.class)))
                    .thenReturn(List.of(profileResponse(userId1, "Max")));
            when(supabaseAdminClient.getListWithQuery(eq("app.profiles"), contains("id=eq." + userId2), eq(ProfileResponse.class)))
                    .thenReturn(List.of(profileResponse(userId2, "Lea")));
            when(supabaseAdminClient.getUserEmail(userId1)).thenReturn("max@example.com");
            when(supabaseAdminClient.getUserEmail(userId2)).thenReturn("lea@example.com");

            reminderService.sendWeeklyReminders();

            verify(reminderMailService).sendReminder("max@example.com", "Max", "Org A", 3, remAppUrl);
            verify(reminderMailService).sendReminder("lea@example.com", "Lea", "Org B", 3, remAppUrl);
            verifyNoMoreInteractions(reminderMailService);
        }

        @Test
        void sendWeeklyReminders_errorForOneUser_continuesWithNext() {
            when(supabaseAdminClient.getListWithQuery(eq("admin.organizations"), anyString(), eq(OrganizationResponse.class)))
                    .thenReturn(List.of(reminderOrgResponse(remOrgId, remOrgName)));
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
            when(supabaseAdminClient.getListWithQuery(eq("admin.organizations"), anyString(), eq(OrganizationResponse.class)))
                    .thenReturn(List.of(reminderOrgResponse(remOrgId, remOrgName)));
            when(supabaseAdminClient.getListWithQuery(eq("admin.organization_members"), anyString(), eq(OrganizationMemberResponse.class)))
                    .thenReturn(List.of(memberResponse(userId1, remOrgId)));
            when(supabaseAdminClient.getListWithQuery(eq("app.activity_records"), contains("entry_date=gte."), eq(ActivityRecordResponse.class)))
                    .thenReturn(List.of());
            var lastRecord = activityRecordResponse(userId1, remOrgId, LocalDate.now().minusDays(10).toString());
            when(supabaseAdminClient.getListWithQuery(eq("app.activity_records"), contains("order=entry_date.desc"), eq(ActivityRecordResponse.class)))
                    .thenReturn(List.of(lastRecord));
            when(supabaseAdminClient.getListWithQuery(eq("app.profiles"), anyString(), eq(ProfileResponse.class)))
                    .thenReturn(List.of(profileResponse(userId1, "Test")));
            when(supabaseAdminClient.getUserEmail(userId1)).thenReturn("test@example.com");

            reminderService.sendWeeklyReminders();

            verify(reminderMailService).sendReminder(eq("test@example.com"), eq("Test"), eq(remOrgName), eq(10L), eq(remAppUrl));
        }

        // --- Reminder helpers ---

        private OrganizationResponse reminderOrgResponse(UUID id, String name) {
            return new OrganizationResponse(id, name, OffsetDateTime.now(), OffsetDateTime.now());
        }

        private OrganizationMemberResponse memberResponse(UUID userId, UUID orgId) {
            return new OrganizationMemberResponse(userId, orgId, "user", OffsetDateTime.now());
        }

        private ProfileResponse profileResponse(UUID id, String firstName) {
            return new ProfileResponse(id, firstName, "Lastname", OffsetDateTime.now(), OffsetDateTime.now());
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
