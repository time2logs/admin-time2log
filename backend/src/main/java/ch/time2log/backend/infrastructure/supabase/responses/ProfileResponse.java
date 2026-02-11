package ch.time2log.backend.infrastructure.supabase.responses;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProfileResponse(
        UUID id,
        String first_name,
        String last_name,
        OffsetDateTime created_at,
        OffsetDateTime updated_at
) {}
