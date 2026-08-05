package ch.time2log.backend.domain.models;

import java.math.BigDecimal;
import java.util.UUID;

public record DailyMemberReport(
        UUID userId,
        String firstName,
        String lastName,
        String status,
        BigDecimal totalHours,
        int recordCount,
        Integer minRating
) {}
