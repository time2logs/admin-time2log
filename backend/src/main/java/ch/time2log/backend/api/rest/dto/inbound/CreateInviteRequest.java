package ch.time2log.backend.api.rest.dto.inbound;

public record CreateInviteRequest(
        String email,
        String userRole
) {}
