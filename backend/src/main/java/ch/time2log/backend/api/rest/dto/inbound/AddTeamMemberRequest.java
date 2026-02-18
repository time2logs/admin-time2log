package ch.time2log.backend.api.rest.dto.inbound;

import java.util.UUID;

public record AddTeamMemberRequest(
        UUID userId,
        String teamRole
) {}
