package ch.time2log.backend.domain.models;

import ch.time2log.backend.infrastructure.supabase.responses.InviteResponse;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record Invite(
        UUID id,
        UUID organizationId,
        String email,
        String userRole,
        UUID token,
        String status,
        UUID invitedBy,
        OffsetDateTime createdAt,
        OffsetDateTime expiresAt
) {

    public static Invite of(InviteResponse r) {
        return new Invite(
                r.id(),
                r.organization_id(),
                r.email(),
                r.user_role(),
                r.token(),
                r.status(),
                r.invited_by(),
                r.created_at(),
                r.expires_at()
        );
    }

    public static List<Invite> ofList(List<InviteResponse> list) {
        return list.stream().map(Invite::of).toList();
    }
}
