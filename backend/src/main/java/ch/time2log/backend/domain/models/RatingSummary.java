package ch.time2log.backend.domain.models;

import java.util.UUID;

public record RatingSummary(
        UUID activityId,
        String activityName,
        double averageRating
) {}