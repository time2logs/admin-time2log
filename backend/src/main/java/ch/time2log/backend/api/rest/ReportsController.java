package ch.time2log.backend.api.rest;

import ch.time2log.backend.api.rest.dto.outbound.DailyMemberReportDto;
import ch.time2log.backend.api.rest.dto.outbound.MemberActivityRecordDto;
import ch.time2log.backend.domain.ReportsDomainService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("api/organizations/{organizationId}/reports")
@RestController
public class ReportsController {

    private final ReportsDomainService reportsDomainService;

    public ReportsController(ReportsDomainService reportsDomainService) {
        this.reportsDomainService = reportsDomainService;
    }

    @GetMapping("/daily")
    public List<DailyMemberReportDto> getDailyReport(
            @PathVariable UUID organizationId,
            @RequestParam String date) {
        return DailyMemberReportDto.ofList(reportsDomainService.getDailyReport(organizationId, date));
    }

    @GetMapping("/members/{userId}/records")
    public List<MemberActivityRecordDto> getMemberRecords(
            @PathVariable UUID organizationId,
            @PathVariable UUID userId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return MemberActivityRecordDto.ofList(reportsDomainService.getMemberRecords(organizationId, userId, date, from, to));
    }
}
