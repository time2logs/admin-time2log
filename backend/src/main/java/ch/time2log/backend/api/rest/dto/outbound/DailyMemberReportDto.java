package ch.time2log.backend.api.rest.dto.outbound;

import ch.time2log.backend.domain.models.DailyMemberReport;

import java.util.List;
import java.util.UUID;

public record DailyMemberReportDto(
        UUID userId,
        String firstName,
        String lastName,
        String status,
        int totalHours,
        int recordCount,
        Integer minRating
) {
    public static DailyMemberReportDto of(DailyMemberReport r) {
        return new DailyMemberReportDto(r.userId(), r.firstName(), r.lastName(), r.status(), r.totalHours(), r.recordCount(), r.minRating());
    }

    public static List<DailyMemberReportDto> ofList(List<DailyMemberReport> list) {
        return list.stream().map(DailyMemberReportDto::of).toList();
    }
}
