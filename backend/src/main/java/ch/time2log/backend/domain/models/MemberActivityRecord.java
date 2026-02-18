package ch.time2log.backend.domain.models;

import java.util.UUID;

public record MemberActivityRecord(
        UUID id,
        String entryDate,
        UUID curriculumActivityId,
        String activityLabel,
        int hours,
        String notes,
        Integer rating
) {}
