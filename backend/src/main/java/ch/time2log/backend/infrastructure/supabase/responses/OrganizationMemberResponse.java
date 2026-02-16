package ch.time2log.backend.infrastructure.supabase.responses;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrganizationMemberResponse(
        UUID user_id,
        UUID organization_id,
        String user_role,
        OffsetDateTime created_at
) {}
