package ch.time2log.backend.domain;

import ch.time2log.backend.domain.exception.EntityNotCreatedException;
import ch.time2log.backend.domain.exception.NoRowsAffectedException;
import ch.time2log.backend.domain.models.Invite;
import ch.time2log.backend.domain.models.Organization;
import ch.time2log.backend.domain.models.Profile;
import ch.time2log.backend.infrastructure.mail.InviteMailService;
import ch.time2log.backend.infrastructure.supabase.SupabaseAdminClient;
import ch.time2log.backend.infrastructure.supabase.SupabaseService;
import ch.time2log.backend.infrastructure.supabase.responses.InviteResponse;
import ch.time2log.backend.infrastructure.supabase.responses.OrganizationMemberResponse;
import ch.time2log.backend.infrastructure.supabase.responses.OrganizationResponse;
import ch.time2log.backend.infrastructure.supabase.responses.ReminderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrganizationDomainService {
    private static final Logger log = LoggerFactory.getLogger(OrganizationDomainService.class);
    private final SupabaseService supabaseService;
    private final SupabaseAdminClient supabaseAdminClient;
    private final ProfileDomainService profileDomainService;
    private final InviteMailService inviteMailService;

    @Value("${app.url}")
    private String appUrl;

    public OrganizationDomainService(SupabaseService supabaseService,
                                     SupabaseAdminClient supabaseAdminClient,
                                     ProfileDomainService profileDomainService,
                                     InviteMailService inviteMailService) {
        this.supabaseService = supabaseService;
        this.supabaseAdminClient = supabaseAdminClient;
        this.profileDomainService = profileDomainService;
        this.inviteMailService = inviteMailService;
    }

    public List<Organization> getOrganizations() {
        var responses = supabaseService.getList("admin.organizations", OrganizationResponse.class);
        return Organization.ofList(responses);
    }

    public Organization createOrganization(String name) {
        var created = supabaseService.post("admin.organizations", Map.of("name", name), OrganizationResponse[].class);
        if (created == null || created.length == 0) {
            throw new EntityNotCreatedException("Supabase returned no created organization");
        }
        return Organization.of(created[0]);
    }

    public void deleteOrganization(UUID id) {
        int deleted = supabaseService.deleteReturningCount("admin.organizations", "id=eq." + id);
        if (deleted == 0) {
            throw new NoRowsAffectedException(
                    HttpStatus.FORBIDDEN,
                    "ORGANIZATION_DELETE_BLOCKED",
                    "Forbidden operation",
                    "Organization could not be deleted."
            );
        }
    }

    public Invite createInvite(UUID organizationId, String email, String userRole, String semester, UUID invitedBy) {
        var body = new HashMap<String, Object>();
        body.put("organization_id", organizationId);
        body.put("email", email);
        body.put("user_role", userRole);
        body.put("invited_by", invitedBy);
        body.put("current_semester", semester);
        log.info("Creating invite with body={}", body);
        var created = supabaseService.post("admin.invites", body, InviteResponse[].class);
        log.info("Supabase returned invite={}", created == null ? null : (created.length == 0 ? "[]" : created[0]));
        if (created == null || created.length == 0) {
            throw new EntityNotCreatedException("Supabase returned no created invite");
        }
        var invite = Invite.of(created[0]);

        var organizations = supabaseService.getListWithQuery(
                "admin.organizations",
                "id=eq." + organizationId,
                OrganizationResponse.class
        );
        var orgName = organizations.isEmpty() ? "the organization" : organizations.get(0).name();
        var redirectTo = appUrl + "/onboarding?invite_token=" + invite.token();
        var metadata = Map.<String, Object>of(
                "invite_token", invite.token().toString(),
                "organization_id", organizationId.toString(),
                "role", userRole
        );
        var actionLink = supabaseAdminClient.generateInviteLink(email, redirectTo, metadata).block();
        inviteMailService.sendInvite(email, orgName, userRole, actionLink);

        return invite;
    }

    public List<Invite> listInvites(UUID organizationId) {
        var responses = supabaseService.getListWithQuery(
                "admin.invites",
                "organization_id=eq." + organizationId + "&order=created_at.desc",
                InviteResponse.class
        );
        return Invite.ofList(responses);
    }

    public void deleteInvite(UUID organizationId, UUID inviteId) {
        int deleted = supabaseService.deleteReturningCount(
                "admin.invites",
                "id=eq." + inviteId + "&organization_id=eq." + organizationId
        );
        if (deleted == 0) {
            throw new NoRowsAffectedException(
                    HttpStatus.FORBIDDEN,
                    "INVITE_DELETE_BLOCKED",
                    "Forbidden operation",
                    "Invite could not be deleted."
            );
        }
    }

    public void removeOrganizationMember(UUID organizationId, UUID userId) {
        int deleted = supabaseService.deleteReturningCount(
                "admin.organization_members",
                "organization_id=eq." + organizationId + "&user_id=eq." + userId
        );
        if (deleted == 0) {
            throw new NoRowsAffectedException(
                    HttpStatus.FORBIDDEN,
                    "MEMBER_REMOVE_BLOCKED",
                    "Forbidden operation",
                    "Organization member could not be removed."
            );
        }
    }

    public List<Profile> getOrganizationMemberProfiles(UUID organizationId) {
        var members = supabaseService.getListWithQuery(
                "admin.organization_members",
                "organization_id=eq." + organizationId,
                OrganizationMemberResponse.class
        );
        var ids = members.stream().map(OrganizationMemberResponse::user_id).toList();
        return profileDomainService.getByIds(ids);
    }

    public List<Profile> getOnlyOrganizationMemberProfiles(UUID organizationId) {
        var members = supabaseService.getListWithQuery(
                "admin.organization_members",
                "organization_id=eq." + organizationId + "&user_role=eq.user",
                OrganizationMemberResponse.class
        );
        var ids = members.stream().map(OrganizationMemberResponse::user_id).toList();
        return profileDomainService.getByIds(ids);
    }

    public ReminderResponse getReminder(UUID organizationId) {
        var list = supabaseService.getListWithQuery(
                "admin.reminder",
                "organization_id=eq." + organizationId,
                ReminderResponse.class
        );
        return list.isEmpty() ? null : list.getFirst();
    }

    public ReminderResponse saveReminder(UUID organizationId, String channel, String sendTime, int idleDays, String sendDay) {
        var existing = getReminder(organizationId);
        var body = Map.of(
                "organization_id", organizationId,
                "channel", channel,
                "send_time", sendTime,
                "idle_days", idleDays,
                "send_day", sendDay
        );
        if (existing != null) {
            var updated = supabaseService.patch(
                    "admin.reminder",
                    "organization_id=eq." + organizationId,
                    body,
                    ReminderResponse[].class
            );
            if (updated == null || updated.length == 0) {
                throw new EntityNotCreatedException("Could not update reminder");
            }
            return updated[0];
        } else {
            var created = supabaseService.post("admin.reminder", body, ReminderResponse[].class);
            if (created == null || created.length == 0) {
                throw new EntityNotCreatedException("Could not create reminder");
            }
            return created[0];
        }
    }
}
