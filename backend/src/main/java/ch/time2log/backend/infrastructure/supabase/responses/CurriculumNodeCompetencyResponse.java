package ch.time2log.backend.infrastructure.supabase.responses;

import java.util.UUID;

public record CurriculumNodeCompetencyResponse(
        UUID curriculum_node_id,
        UUID competency_id
) {}
