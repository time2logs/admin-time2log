package ch.time2log.backend.domain.models;

import ch.time2log.backend.infrastructure.supabase.responses.CurriculumNodeResponse;

import java.util.List;
import java.util.UUID;

public record CurriculumNode(
        UUID id,
        UUID parentId,
        String nodeType,
        String key,
        String label,
        int sortOrder,
        List<UUID> competencyIds
) {
    public static CurriculumNode of(CurriculumNodeResponse r, List<UUID> competencyIds) {
        return new CurriculumNode(r.id(), r.parent_id(), r.node_type(), r.key(), r.label(), r.sort_order(), competencyIds);
    }
}
