package ch.time2log.backend.infrastructure.supabase.responses;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TeamMemberResponse(
        UUID team_id,
        UUID user_id,
        String team_role,
        OffsetDateTime created_at
) {}
