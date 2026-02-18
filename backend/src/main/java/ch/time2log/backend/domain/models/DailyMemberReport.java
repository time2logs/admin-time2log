package ch.time2log.backend.domain.models;

import java.util.UUID;

public record DailyMemberReport(
        UUID userId,
        String firstName,
        String lastName,
        String status,
        int totalHours,
        int recordCount,
        Integer minRating
) {}
