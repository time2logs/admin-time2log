package ch.time2log.backend.domain;

import ch.time2log.backend.domain.exception.EntityNotCreatedException;
import ch.time2log.backend.domain.exception.NoRowsAffectedException;
import ch.time2log.backend.domain.models.Invite;
import ch.time2log.backend.domain.models.Profile;
import ch.time2log.backend.infrastructure.mail.InviteMailService;
import ch.time2log.backend.infrastructure.supabase.SupabaseAdminClient;
import ch.time2log.backend.infrastructure.supabase.SupabaseService;
import ch.time2log.backend.infrastructure.supabase.responses.InviteResponse;
import ch.time2log.backend.infrastructure.supabase.responses.ProfileResponse;
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
public class SystemAdminDomainService {
    private static final Logger log = LoggerFactory.getLogger(SystemAdminDomainService.class);

    private static final String PLATFORM_ROLE = "admin";
    private static final String PLATFORM_NAME = "Time2log";

    private final SupabaseService supabaseService;
    private final SupabaseAdminClient supabaseAdminClient;
    private final InviteMailService inviteMailService;
    private final ProfileDomainService profileDomainService;

    @Value("${app.url.admin}")
    private String adminAppUrl;

    public SystemAdminDomainService(SupabaseService supabaseService,
                                    SupabaseAdminClient supabaseAdminClient,
                                    InviteMailService inviteMailService,
                                    ProfileDomainService profileDomainService) {
        this.supabaseService = supabaseService;
        this.supabaseAdminClient = supabaseAdminClient;
        this.inviteMailService = inviteMailService;
        this.profileDomainService = profileDomainService;
    }

    public Invite createPlatformInvite(String email, UUID invitedBy) {
        var body = new HashMap<String, Object>();
        body.put("organization_id", null);
        body.put("email", email);
        body.put("user_role", PLATFORM_ROLE);
        body.put("invited_by", invitedBy);
        body.put("current_semester", null);

        log.info("Creating platform invite");
        var created = supabaseService.post("admin.invites", body, InviteResponse[].class);
        if (created == null || created.length == 0) {
            throw new EntityNotCreatedException("Supabase returned no created platform invite");
        }
        var invite = Invite.of(created[0]);

        var redirectTo = adminAppUrl + "/auth/onboarding?invite_token=" + invite.token();
        var metadata = Map.<String, Object>of(
                "invite_token", invite.token().toString(),
                "role", PLATFORM_ROLE
        );
        var actionLink = supabaseAdminClient.generateInviteLink(email, redirectTo, metadata).block();
        inviteMailService.sendInvite(email, PLATFORM_NAME, PLATFORM_ROLE, actionLink);

        return invite;
    }

    public List<Profile> getAdmins() {
        var profiles = supabaseService.getListWithQuery(
                "app.profiles",
                "user_role=in.(admin,system_admin)&order=user_role.desc,first_name.asc",
                ProfileResponse.class
        );
        return Profile.ofList(profiles);
    }

    public List<Profile> getModerators() {
        var profiles = supabaseService.getListWithQuery(
                "app.profiles",
                "user_role=eq.moderator&order=first_name.asc",
                ProfileResponse.class
        );
        return Profile.ofList(profiles);
    }

    public List<Invite> listInvites(String userRole) {
        var responses = supabaseService.getListWithQuery(
                "admin.invites",
                "user_role=eq." + userRole + "&status=eq.pending&order=created_at.desc",
                InviteResponse.class
        );
        return Invite.ofList(responses);
    }

    public void deleteInvite(UUID inviteId) {
        var existing = supabaseService.getListWithQuery(
                "admin.invites",
                "id=eq." + inviteId,
                InviteResponse.class
        );

        int deleted = supabaseService.deleteReturningCount(
                "admin.invites",
                "id=eq." + inviteId
        );
        if (deleted == 0) {
            throw new NoRowsAffectedException(
                    HttpStatus.FORBIDDEN,
                    "INVITE_DELETE_BLOCKED",
                    "Forbidden operation",
                    "Invite could not be deleted."
            );
        }

        if (existing != null && !existing.isEmpty()) {
            var invite = existing.get(0);
            if ("pending".equalsIgnoreCase(invite.status())) {
                deleteAuthUserForPendingInvite(invite.email());
            }
        }
    }

    /**
     * Supabase's generate_link creates a real auth.users row at invite time. If the
     * admin revokes the invite while it's still pending, that auth user is dangling:
     * the email link still resolves to a valid Supabase session, which would let the
     * recipient bypass the deleted invite. Remove the auth user too. Only safe while
     * status='pending' — once accepted, the same auth user backs a real profile.
     */
    private void deleteAuthUserForPendingInvite(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        try {
            UUID userId = supabaseAdminClient.getUserIdByEmail(email);
            if (userId == null) return;
            if (profileDomainService.existsByEmail(email)) {
                log.info("Skipping auth user delete for revoked platform invite: profile exists");
                return;
            }
            supabaseAdminClient.deleteUser(userId).block();
        } catch (Exception ex) {
            log.warn("Failed to delete dangling auth user for revoked invite: {}", ex.getMessage());
        }
    }
}
