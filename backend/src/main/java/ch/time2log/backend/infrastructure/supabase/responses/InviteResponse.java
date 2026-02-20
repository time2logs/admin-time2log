package ch.time2log.backend.infrastructure.supabase.responses;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InviteResponse(
        UUID id,
        UUID organization_id,
        String email,
        String user_role,
        UUID token,
        String status,
        UUID invited_by,
        OffsetDateTime created_at,
        OffsetDateTime expires_at
) {}
