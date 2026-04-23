package ch.time2log.backend.domain;

import ch.time2log.backend.domain.models.ActivitySummary;
import ch.time2log.backend.domain.models.DailyMemberReport;
import ch.time2log.backend.domain.models.MemberActivityRecord;
import ch.time2log.backend.infrastructure.supabase.SupabaseService;
import ch.time2log.backend.infrastructure.supabase.responses.ActivityRecordResponse;
import ch.time2log.backend.infrastructure.supabase.responses.CurriculumNodeResponse;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Comparator;
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
                    "admin.curriculum_nodes",
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
                r.rating(),
                r.team_id()
        )).toList();
    }
    public List<ActivitySummary> getActivitySummary(UUID organizationId, UUID userId, String from, String to, List<String> semesters) {
        if (userId == null) {
            return List.of();
        }

        String query = "organization_id=eq." + organizationId + "&user_id=eq." + userId;

        if (semesters != null && !semesters.isEmpty()) {
            query += "&current_semester=in.(" + semesters.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(",")) + ")";
        } else {
            if (from != null && !from.isBlank()) query += "&entry_date=gte." + from;
            if (to != null && !to.isBlank()) query += "&entry_date=lte." + to;
        }

        var records = supabaseService.getListWithQuery("app.activity_records", query, ActivityRecordResponse.class);
        if (records.isEmpty()) return List.of();

        Map<UUID, Integer> hoursByActivity = records.stream()
                .filter(r -> r.curriculum_activity_id() != null)
                .collect(Collectors.groupingBy(
                        ActivityRecordResponse::curriculum_activity_id,
                        Collectors.summingInt(ActivityRecordResponse::hours)
                ));

        if (hoursByActivity.isEmpty()) return List.of();

        var activityIds = hoursByActivity.keySet().stream()
                .map(UUID::toString)
                .collect(Collectors.joining(","));

        var nodes = supabaseService.getListWithQuery(
                "admin.curriculum_nodes",
                "id=in.(" + activityIds + ")",
                CurriculumNodeResponse.class
        );

        var labelMap = nodes.stream()
                .collect(Collectors.toMap(CurriculumNodeResponse::id, CurriculumNodeResponse::label));

        return hoursByActivity.entrySet().stream()
                .map(e -> new ActivitySummary(
                        e.getKey(),
                        labelMap.getOrDefault(e.getKey(), "Unbekannt"),
                        e.getValue()
                ))
                .sorted(Comparator.comparingInt(ActivitySummary::totalHours).reversed())
                .toList();
    }

    public Map<String, Integer> getLocationSummary(UUID organizationId, UUID userId, String from, String to, List<String> semesters) {
        if (userId == null) return Map.of();

        String query = "organization_id=eq." + organizationId + "&user_id=eq." + userId;
        if (semesters != null && !semesters.isEmpty()) {
            query += "&current_semester=in.(" + semesters.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(",")) + ")";
        } else {
            if (from != null && !from.isBlank()) query += "&entry_date=gte." + from;
            if (to != null && !to.isBlank()) query += "&entry_date=lte." + to;
        }

        return supabaseService.getListWithQuery("app.activity_records", query, ActivityRecordResponse.class)
                .stream()
                .filter(r -> r.location() != null && !r.location().isBlank())
                .collect(Collectors.groupingBy(ActivityRecordResponse::location,
                        Collectors.summingInt(ActivityRecordResponse::hours)));
    }

    public List<String> getAvailableSemesters(UUID organizationId, UUID userId) {
        if (userId == null) return List.of();
        var records = supabaseService.getListWithQuery(
                "app.activity_records",
                "organization_id=eq." + organizationId + "&user_id=eq." + userId + "&select=current_semester",
                ActivityRecordResponse.class
        );
        return records.stream()
                .map(ActivityRecordResponse::current_semester)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    public OffsetDateTime getLastEntryDate(UUID organizationId, UUID userId) {
        var records = supabaseService.getListWithQuery(
                "app.activity_records",
                "organization_id=eq." + organizationId + "&user_id=eq." + userId + "&order=entry_date.desc&limit=1",
                ActivityRecordResponse.class
        );

        if (records.isEmpty()) return null;

        return records.getFirst().created_at();
    }
}
