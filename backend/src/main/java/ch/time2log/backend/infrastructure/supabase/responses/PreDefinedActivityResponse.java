package ch.time2log.backend.infrastructure.supabase.responses;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record PreDefinedActivityResponse(
        UUID id,
        UUID organization_id,
        String key,
        String label,
        String description,
        String category,
        Map<String, Object> meta,
        boolean is_active,
        OffsetDateTime created_at,
        OffsetDateTime updated_at
) {}
