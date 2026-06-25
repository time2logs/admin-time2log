package ch.time2log.backend.infrastructure.supabase.responses;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        UUID created_by,
        OffsetDateTime created_at,
        OffsetDateTime updated_at,
        java.time.LocalDate semester_end_date,
        java.math.BigDecimal target_hours
) {}