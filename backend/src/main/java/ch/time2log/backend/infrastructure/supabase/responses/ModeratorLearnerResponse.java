package ch.time2log.backend.infrastructure.supabase.responses;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ModeratorLearnerResponse(
        UUID organization_id,
        UUID moderator_id,
        UUID learner_id,
        OffsetDateTime created_at
) {}
