package ch.time2log.backend.domain.models;

import java.math.BigDecimal;
import java.util.UUID;

public record ActivitySummary(
        UUID activityId,
        String activityName,
        BigDecimal totalHours
) {}
