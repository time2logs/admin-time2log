package ch.time2log.backend.infrastructure.supabase.responses;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CurriculumImportResponse(
        UUID id,
        UUID organization_id,
        UUID profession_id,
        UUID uploaded_by,
        Object payload,
        String version,
        String status,
        String error,
        OffsetDateTime created_at
) {}
