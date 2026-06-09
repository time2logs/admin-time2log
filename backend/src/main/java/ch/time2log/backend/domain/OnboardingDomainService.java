package ch.time2log.backend.domain;

import ch.time2log.backend.domain.exception.EntityNotFoundException;
import ch.time2log.backend.infrastructure.supabase.SupabaseAdminClient;
import ch.time2log.backend.infrastructure.supabase.SupabaseService;
import ch.time2log.backend.infrastructure.supabase.responses.InviteDetailsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class OnboardingDomainService {

    private static final Logger log = LoggerFactory.getLogger(OnboardingDomainService.class);
    private static final Set<String> ADMIN_ROLES = Set.of("admin", "system_admin", "moderator");

    private final SupabaseAdminClient adminClient;
    private final SupabaseService supabaseService;

    public OnboardingDomainService(SupabaseAdminClient adminClient, SupabaseService supabaseService) {
        this.adminClient = adminClient;
        this.supabaseService = supabaseService;
    }

    /**
     * Resolves invite details (org name, email, role) for prefilling the onboarding form.
     */
    public InviteDetailsResponse getInviteDetails(String token) {
        var details = adminClient.callRpc(
                "admin", "get_invite_details",
                Map.of("invite_token", token),
                InviteDetailsResponse.class
        );
        if (details == null || details.email() == null) {
            throw new EntityNotFoundException("Invite not found or no longer valid");
        }
        return details;
    }

    /**
     * Completes the admin onboarding: sets the password + name on the auth user,
     * accepts the invite (org membership + profile) via service-role RPC.
     */
    public void complete(String token, String firstName, String lastName, String password) {
        var details = getInviteDetails(token);

        if (!ADMIN_ROLES.contains(details.role())) {
            throw new IllegalStateException("This invite is not for an admin account");
        }

        var email = details.email();
        UUID userId = adminClient.getUserIdByEmail(email);
        if (userId == null) {
            // Fallback: auth user not present (invite link normally creates it).
            userId = adminClient.createUser(email, true);
        }

        adminClient.updateUser(userId, password, firstName, lastName);

        adminClient.callRpc(
                "admin", "accept_invite_for",
                Map.of(
                        "p_user_id", userId,
                        "invite_token", token,
                        "p_first_name", firstName,
                        "p_last_name", lastName
                ),
                Void.class
        );

        log.info("Completed admin onboarding for {}", email);
    }

    public void acceptInviteAsExistingUser(UUID token) {
        if (token == null) {
            throw new EntityNotFoundException("Invite token is required");
        }
        supabaseService.rpc(
                "admin.accept_org_invite_for_existing_user",
                Map.of("invite_token", token),
                Void.class
        );
    }
}
