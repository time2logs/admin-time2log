package ch.time2log.backend.api.rest;

import ch.time2log.backend.api.rest.dto.outbound.ActivitySummaryDto;
import ch.time2log.backend.api.rest.dto.outbound.DailyMemberReportDto;
import ch.time2log.backend.api.rest.dto.outbound.LocationSummaryDto;
import ch.time2log.backend.api.rest.dto.outbound.MemberActivityRecordDto;
import ch.time2log.backend.domain.ReportsDomainService;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
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

    @GetMapping("/activities/summary")
    public List<ActivitySummaryDto> getActivitySummary(
            @PathVariable UUID organizationId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) List<String> semesters) {
        return ActivitySummaryDto.ofList(reportsDomainService.getActivitySummary(organizationId, userId, from, to, semesters));
    }

    @GetMapping("/locations/summary")
    public List<LocationSummaryDto> getLocationSummary(
            @PathVariable UUID organizationId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) List<String> semesters) {
        return LocationSummaryDto.ofMap(reportsDomainService.getLocationSummary(organizationId, userId, from, to, semesters));
    }

    @GetMapping("/semesters/available")
    public List<String> getAvailableSemesters(
            @PathVariable UUID organizationId,
            @RequestParam UUID userId) {
        return reportsDomainService.getAvailableSemesters(organizationId, userId);
    }

    @GetMapping("/members/{userId}/last-entry-date")
    public OffsetDateTime getLastEntryDate(
            @PathVariable UUID organizationId,
            @PathVariable UUID userId){
        return reportsDomainService.getLastEntryDate(organizationId, userId);
    }
}
