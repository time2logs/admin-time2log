package ch.time2log.backend.infrastructure.supabase.responses;

/**
 * Shape of the JSON returned by the admin.get_invite_details RPC.
 */
public record InviteDetailsResponse(
        String organization_name,
        String email,
        String role
) {}
