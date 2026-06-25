package ch.time2log.backend.infrastructure.supabase.responses;

import java.util.UUID;

public record AbsenceResponse(
   UUID id,
   UUID organization_id,
   UUID user_id,
   UUID team_id,
   String absence_type_id,
   String start_date,
   String end_date,
   String rrule,
   boolean is_recurring,
   Double day_fraction,
   String notes,
   String current_semester
) {}
