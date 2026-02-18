package ch.time2log.backend.api.rest.dto.outbound;

import ch.time2log.backend.domain.models.Competency;

import java.util.UUID;

public record CompetencyDto(
        UUID id,
        String code,
        String description
) {
    public static CompetencyDto of(Competency c) {
        return new CompetencyDto(c.id(), c.code(), c.description());
    }
}
