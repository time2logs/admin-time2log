package ch.time2log.backend.infrastructure.supabase.responses;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        UUID created_by,
        OffsetDateTime created_at,
        OffsetDateTime updated_at
) {}