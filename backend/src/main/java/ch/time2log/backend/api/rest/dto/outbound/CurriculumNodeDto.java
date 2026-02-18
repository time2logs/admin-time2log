package ch.time2log.backend.api.rest.dto.outbound;

import ch.time2log.backend.domain.models.CurriculumNode;

import java.util.List;
import java.util.UUID;

public record CurriculumNodeDto(
        UUID id,
        UUID parentId,
        String nodeType,
        String key,
        String label,
        int sortOrder,
        List<UUID> competencyIds
) {
    public static CurriculumNodeDto of(CurriculumNode n) {
        return new CurriculumNodeDto(n.id(), n.parentId(), n.nodeType(), n.key(), n.label(), n.sortOrder(), n.competencyIds());
    }
}
