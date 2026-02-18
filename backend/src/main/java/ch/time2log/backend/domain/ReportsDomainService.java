package ch.time2log.backend.domain;

import ch.time2log.backend.domain.models.DailyMemberReport;
import ch.time2log.backend.domain.models.MemberActivityRecord;
import ch.time2log.backend.infrastructure.supabase.SupabaseService;
import ch.time2log.backend.infrastructure.supabase.responses.ActivityRecordResponse;
import ch.time2log.backend.infrastructure.supabase.responses.CurriculumNodeResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReportsDomainService {

    private final SupabaseService supabaseService;
    private final OrganizationDomainService organizationDomainService;

    public ReportsDomainService(SupabaseService supabaseService, OrganizationDomainService organizationDomainService) {
        this.supabaseService = supabaseService;
        this.organizationDomainService = organizationDomainService;
    }

    public List<DailyMemberReport> getDailyReport(UUID organizationId, String date) {
        var profiles = organizationDomainService.getOrganizationMemberProfiles(organizationId);

        var records = supabaseService.getListWithQuery(
                "app.activity_records",
                "organization_id=eq." + organizationId + "&entry_date=eq." + date,
                ActivityRecordResponse.class
        );

        Map<UUID, List<ActivityRecordResponse>> byUser = records.stream()
                .collect(Collectors.groupingBy(ActivityRecordResponse::user_id));

        return profiles.stream().map(profile -> {
            var userRecords = byUser.getOrDefault(profile.id(), List.of());
            String status;
            int totalHours = 0;
            Integer minRating = null;

            if (userRecords.isEmpty()) {
                status = "missing";
            } else {
                totalHours = userRecords.stream().mapToInt(ActivityRecordResponse::hours).sum();
                var ratings = userRecords.stream()
                        .filter(r -> r.rating() != null)
                        .mapToInt(ActivityRecordResponse::rating)
                        .boxed()
                        .toList();
                if (!ratings.isEmpty()) {
                    minRating = ratings.stream().mapToInt(Integer::intValue).min().orElse(0);
                }
                boolean hasBadRating = ratings.stream().anyMatch(r -> r <= 1);
                status = hasBadRating ? "bad_rating" : "reported";
            }

            return new DailyMemberReport(profile.id(), profile.firstName(), profile.lastName(), status, totalHours, userRecords.size(), minRating);
        }).toList();
    }

    public List<MemberActivityRecord> getMemberRecords(UUID organizationId, UUID userId, String date, String from, String to) {
        String query = "organization_id=eq." + organizationId + "&user_id=eq." + userId;
        if (date != null && !date.isBlank()) {
            query += "&entry_date=eq." + date;
        } else {
            if (from != null && !from.isBlank()) query += "&entry_date=gte." + from;
            if (to != null && !to.isBlank()) query += "&entry_date=lte." + to;
        }
        query += "&order=entry_date.asc";

        var records = supabaseService.getListWithQuery("app.activity_records", query, ActivityRecordResponse.class);
        if (records.isEmpty()) return List.of();

        var activityIds = records.stream()
                .filter(r -> r.curriculum_activity_id() != null)
                .map(r -> r.curriculum_activity_id().toString())
                .distinct()
                .collect(Collectors.joining(","));

        Map<UUID, String> labelMap = Map.of();
        if (!activityIds.isBlank()) {
            var nodes = supabaseService.getListWithQuery(
                    "app.curriculum_nodes",
                    "id=in.(" + activityIds + ")",
                    CurriculumNodeResponse.class
            );
            labelMap = nodes.stream().collect(Collectors.toMap(CurriculumNodeResponse::id, CurriculumNodeResponse::label));
        }

        final var finalLabelMap = labelMap;
        return records.stream().map(r -> new MemberActivityRecord(
                r.id(),
                r.entry_date(),
                r.curriculum_activity_id(),
                r.curriculum_activity_id() != null ? finalLabelMap.getOrDefault(r.curriculum_activity_id(), "") : "",
                r.hours(),
                r.notes(),
                r.rating()
        )).toList();
    }
}
