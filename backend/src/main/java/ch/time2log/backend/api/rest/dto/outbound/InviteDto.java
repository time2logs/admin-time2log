package ch.time2log.backend.api.rest.dto.outbound;

import ch.time2log.backend.domain.models.Invite;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record InviteDto(
        UUID id,
        UUID organizationId,
        String email,
        String userRole,
        UUID token,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime expiresAt
) {

    public static InviteDto of(Invite invite) {
        return new InviteDto(
                invite.id(),
                invite.organizationId(),
                invite.email(),
                invite.userRole(),
                invite.token(),
                invite.status(),
                invite.createdAt(),
                invite.expiresAt()
        );
    }

    public static List<InviteDto> ofList(List<Invite> list) {
        return list.stream().map(InviteDto::of).toList();
    }
}
