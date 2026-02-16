package ch.time2log.backend.api.rest.dto.inbound;

import java.util.UUID;

public record InviteRequest(
        UUID userId,
        String userRole
) {}
