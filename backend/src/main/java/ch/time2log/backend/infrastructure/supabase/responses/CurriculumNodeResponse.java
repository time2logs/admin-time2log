package ch.time2log.backend.infrastructure.supabase.responses;

import java.util.UUID;

public record CurriculumNodeResponse(
   UUID id,
   UUID organization_id,
   UUID profession_id,
   UUID parent_id,
   String node_type,
   String key,
   String label,
   String description,
   int sort_order,
   boolean is_active
) {}
