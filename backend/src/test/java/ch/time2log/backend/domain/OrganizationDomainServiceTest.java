package ch.time2log.backend.domain;

import ch.time2log.backend.domain.exception.EntityNotCreatedException;
import ch.time2log.backend.domain.exception.NoRowsAffectedException;
import ch.time2log.backend.domain.models.Invite;
import ch.time2log.backend.domain.models.Organization;
import ch.time2log.backend.infrastructure.mail.InviteMailService;
import ch.time2log.backend.infrastructure.supabase.SupabaseAdminClient;
import ch.time2log.backend.infrastructure.supabase.SupabaseService;
import ch.time2log.backend.infrastructure.supabase.responses.InviteResponse;
import ch.time2log.backend.infrastructure.supabase.responses.OrganizationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

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

        Invite result = organizationDomainService.createInvite(orgId, email, userRole, invitedBy);

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

        organizationDomainService.createInvite(orgId, email, userRole, invitedBy);

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

        organizationDomainService.createInvite(orgId, email, userRole, invitedBy);

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

        organizationDomainService.createInvite(orgId, email, userRole, invitedBy);

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

        assertThatThrownBy(() -> organizationDomainService.createInvite(orgId, email, userRole, invitedBy))
                .isInstanceOf(EntityNotCreatedException.class);
    }

    @Test
    void createInvite_whenSupabaseReturnsEmptyArray_throwsEntityNotCreatedException() {
        when(supabaseService.post(eq("admin.invites"), any(), eq(InviteResponse[].class)))
                .thenReturn(new InviteResponse[]{});

        assertThatThrownBy(() -> organizationDomainService.createInvite(orgId, email, userRole, invitedBy))
                .isInstanceOf(EntityNotCreatedException.class);
    }

    // --- listInvites ---

    @Test
    void listInvites_returnsMappedList() {
        var invite1 = inviteResponse();
        var invite2 = new InviteResponse(UUID.randomUUID(), orgId, "other@example.com", "admin",
                UUID.randomUUID(), "pending", invitedBy, OffsetDateTime.now(), OffsetDateTime.now().plusDays(7));
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
                invitedBy, OffsetDateTime.now(), OffsetDateTime.now().plusDays(7));
    }

    private OrganizationResponse orgResponse(String name) {
        return new OrganizationResponse(orgId, name, OffsetDateTime.now(), OffsetDateTime.now());
    }
}
