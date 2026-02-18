package ch.time2log.backend.infrastructure.supabase.responses;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProfessionResponse(
        UUID id,
        UUID organization_id,
        String key,
        String label,
        OffsetDateTime created_at,
        OffsetDateTime updated_at
) {}
