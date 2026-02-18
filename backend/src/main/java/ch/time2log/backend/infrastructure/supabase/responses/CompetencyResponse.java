package ch.time2log.backend.infrastructure.supabase.responses;

import java.util.UUID;

public record CompetencyResponse(
        UUID id,
        UUID organization_id,
        UUID profession_id,
        String code,
        String description
) {}
