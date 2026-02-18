package ch.time2log.backend.infrastructure.supabase.responses;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ActivityRecordResponse(
   UUID id,
   UUID organization_id,
   UUID user_id,
   UUID team_id,
   UUID curriculum_activity_id,
   String entry_date,
   int hours,
   String notes,
   Integer rating,
   OffsetDateTime created_at,
   OffsetDateTime updated_at
) {}
