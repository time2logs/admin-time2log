package ch.time2log.backend.domain.models;

import java.math.BigDecimal;
import java.util.UUID;

public record MemberActivityRecord(
        UUID id,
        String entryDate,
        UUID curriculumActivityId,
        String activityLabel,
        BigDecimal hours,
        String notes,
        Integer rating,
        UUID teamId,
        String location
) {}
