package ch.time2log.backend.domain;

import ch.time2log.backend.domain.models.ActivitySummary;
import ch.time2log.backend.domain.models.DailyMemberReport;
import ch.time2log.backend.domain.models.MemberAbsence;
import ch.time2log.backend.domain.models.MemberActivityRecord;
import ch.time2log.backend.domain.models.Profile;
import ch.time2log.backend.domain.models.RatingSummary;
import ch.time2log.backend.infrastructure.supabase.SupabaseService;
import ch.time2log.backend.infrastructure.supabase.responses.AbsenceResponse;
import ch.time2log.backend.infrastructure.supabase.responses.ActivityRecordResponse;
import ch.time2log.backend.infrastructure.supabase.responses.CurriculumNodeResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
        Number targetHoursNumber = organizationDomainService.getTargetHours(organizationId);
        BigDecimal targetHours = BigDecimal.valueOf(targetHoursNumber == null ? 8 : targetHoursNumber.doubleValue());

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
            BigDecimal totalHours = BigDecimal.ZERO.setScale(2);
            Integer minRating = null;

            if (userRecords.isEmpty()) {
                status = "missing";
            } else {
                totalHours = userRecords.stream()
                        .map(ActivityRecordResponse::hours)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP);
                var ratings = userRecords.stream()
                        .filter(r -> r.rating() != null)
                        .mapToInt(ActivityRecordResponse::rating)
                        .boxed()
                        .toList();
                if (!ratings.isEmpty()) {
                    minRating = ratings.stream().mapToInt(Integer::intValue).min().orElse(0);
                }
                boolean hasBadRating = ratings.stream().anyMatch(r -> r <= 2);
                boolean underTarget = totalHours.compareTo(targetHours) < 0;
                if (hasBadRating && underTarget) {
                    status = "bad_rating_under_target";
                } else if (hasBadRating) {
                    status = "bad_rating";
                } else if (underTarget) {
                    status = "under_target";
                } else {
                    status = "reported";
                }
            }

            return new DailyMemberReport(profile.id(), profile.firstName(), profile.lastName(), status, totalHours, userRecords.size(), minRating);
        }).toList();
    }

    public List<MemberActivityRecord> getMemberRecords(UUID organizationId, UUID userId, String date, String from, String to, String location) {
        String query = "organization_id=eq." + organizationId + "&user_id=eq." + userId;
        if (date != null && !date.isBlank()) {
            query += "&entry_date=eq." + date;
        } else {
            if (from != null && !from.isBlank()) query += "&entry_date=gte." + from;
            if (to != null && !to.isBlank()) query += "&entry_date=lte." + to;
        }
        query += "&order=entry_date.asc";

        var records = supabaseService.getListWithQuery("app.activity_records", query, ActivityRecordResponse.class);
        String normalizedLocation = location == null ? "" : location.trim();
        if (!normalizedLocation.isBlank()) {
            records = records.stream()
                    .filter(r -> r.location() != null && r.location().trim().equalsIgnoreCase(normalizedLocation))
                    .toList();
        }
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
                r.team_id(),
                r.location()
        )).toList();
    }
    public List<ActivitySummary> getActivitySummary(UUID organizationId, UUID userId, UUID professionId, String from, String to, List<String> semesters) {
        if (userId == null && professionId == null) {
            return List.of();
        }

        var records = fetchFilteredRecords(organizationId, userId, professionId, from, to, semesters);
        if (records.isEmpty()) return List.of();

        Map<UUID, BigDecimal> hoursByActivity = records.stream()
                .filter(r -> r.curriculum_activity_id() != null)
                .collect(Collectors.groupingBy(
                        ActivityRecordResponse::curriculum_activity_id,
                        Collectors.reducing(BigDecimal.ZERO, ActivityRecordResponse::hours, BigDecimal::add)
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

        var displayLabels = buildActivityLabels(nodes);

        return hoursByActivity.entrySet().stream()
                .map(e -> new ActivitySummary(
                        e.getKey(),
                        displayLabels.getOrDefault(e.getKey(), "Unbekannt"),
                        e.getValue().setScale(2, RoundingMode.HALF_UP)
                ))
                .sorted(Comparator.comparing(ActivitySummary::totalHours).reversed())
                .toList();
    }

    public Map<String, BigDecimal> getLocationSummary(UUID organizationId, UUID userId, UUID professionId, String from, String to, List<String> semesters) {
        if (userId == null && professionId == null) return Map.of();

        return fetchFilteredRecords(organizationId, userId, professionId, from, to, semesters)
                .stream()
                .filter(r -> r.location() != null && !r.location().isBlank())
                .collect(Collectors.groupingBy(ActivityRecordResponse::location,
                        Collectors.reducing(BigDecimal.ZERO, ActivityRecordResponse::hours, BigDecimal::add)))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().setScale(2, RoundingMode.HALF_UP)
                ));
    }

    public List<String> getAvailableSemesters(UUID organizationId, UUID userId) {
        if (userId == null) return List.of();
        // Kein &select=current_semester: die Teil-Projektion ließe das primitive
        // Feld `hours` in ActivityRecordResponse fehlen, was die Deserialisierung
        // (Record mit primitivem int) zum Scheitern bringt. Volle Zeilen laden.
        var records = supabaseService.getListWithQuery(
                "app.activity_records",
                "organization_id=eq." + organizationId + "&user_id=eq." + userId,
                ActivityRecordResponse.class
        );
        return records.stream()
                .map(ActivityRecordResponse::current_semester)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    public List<MemberAbsence> getMemberAbsences(UUID organizationId, UUID userId, List<String> semesters) {
        if (userId == null) return List.of();

        String query = "organization_id=eq." + organizationId + "&user_id=eq." + userId;
        if (semesters != null && !semesters.isEmpty()) {
            query += "&current_semester=in.(" + semesters.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(",")) + ")";
        }
        query += "&order=start_date.desc";

        var responses = supabaseService.getListWithQuery("app.absences", query, AbsenceResponse.class);
        return MemberAbsence.ofList(responses);
    }

    public LocalDate getLastEntryDate(UUID organizationId, UUID userId) {
        LocalDate lastActivity = getLastActivityDate(organizationId, userId);
        LocalDate lastAbsence = getLastAbsenceDate(organizationId, userId);

        if (lastActivity == null) return lastAbsence;
        if (lastAbsence == null) return lastActivity;
        return lastActivity.isAfter(lastAbsence) ? lastActivity : lastAbsence;
    }

    private LocalDate getLastActivityDate(UUID organizationId, UUID userId) {
        var records = supabaseService.getListWithQuery(
                "app.activity_records",
                "organization_id=eq." + organizationId + "&user_id=eq." + userId + "&order=entry_date.desc&limit=1",
                ActivityRecordResponse.class
        );

        if (records.isEmpty()) return null;

        return parseDate(records.getFirst().entry_date());
    }

    private LocalDate getLastAbsenceDate(UUID organizationId, UUID userId) {
        LocalDate today = LocalDate.now();

        var absences = supabaseService.getListWithQuery(
                "app.absences",
                "organization_id=eq." + organizationId + "&user_id=eq." + userId
                        + "&start_date=lte." + today + "&order=end_date.desc&limit=1",
                AbsenceResponse.class
        );

        if (absences.isEmpty()) return null;

        LocalDate endDate = parseDate(absences.getFirst().end_date());
        if (endDate == null) return null;

        return endDate.isAfter(today) ? today : endDate;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDate.parse(value);
    }

    public List<RatingSummary> getRatingSummary(UUID organizationId, UUID userId, UUID professionId, String from, String to, List<String> semesters) {
        if (userId == null && professionId == null) return List.of();

        var records = fetchFilteredRecords(organizationId, userId, professionId, from, to, semesters);
        if (records.isEmpty()) return List.of();

        // Gruppiere ratings pro Aktivität und berechne Durchschnitt
        Map<UUID, List<Integer>> ratingsByActivity = records.stream()
                .filter(r -> r.curriculum_activity_id() != null && r.rating() != null)
                .collect(Collectors.groupingBy(
                        ActivityRecordResponse::curriculum_activity_id,
                        Collectors.mapping(ActivityRecordResponse::rating, Collectors.toList())
                ));

        if (ratingsByActivity.isEmpty()) return List.of();

        var activityIds = ratingsByActivity.keySet().stream()
                .map(UUID::toString)
                .collect(Collectors.joining(","));

        var nodes = supabaseService.getListWithQuery(
                "admin.curriculum_nodes",
                "id=in.(" + activityIds + ")",
                CurriculumNodeResponse.class
        );

        var displayLabels = buildActivityLabels(nodes);

        return ratingsByActivity.entrySet().stream()
                .map(e -> new RatingSummary(
                        e.getKey(),
                        displayLabels.getOrDefault(e.getKey(), "Unbekannt"),
                        e.getValue().stream().mapToInt(Integer::intValue).average().orElse(0)
                ))
                .sorted(Comparator.comparingDouble(RatingSummary::averageRating).reversed())
                .toList();
    }

    private List<ActivityRecordResponse> fetchFilteredRecords(UUID organizationId, UUID userId, UUID professionId, String from, String to, List<String> semesters) {
        // Beruf-Filter vorab auflösen (kann leer sein → keine passenden Records).
        Set<UUID> professionActivityIds = null;
        if (professionId != null) {
            professionActivityIds = getProfessionActivityIds(organizationId, professionId);
            if (professionActivityIds.isEmpty()) return List.of();
        }

        String query = "organization_id=eq." + organizationId;
        if (userId != null) query += "&user_id=eq." + userId;

        if (semesters != null && !semesters.isEmpty()) {
            query += "&current_semester=in.(" + semesters.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(",")) + ")";
        } else {
            if (from != null && !from.isBlank()) query += "&entry_date=gte." + from;
            if (to != null && !to.isBlank()) query += "&entry_date=lte." + to;
        }

        var records = supabaseService.getListWithQuery("app.activity_records", query, ActivityRecordResponse.class);

        if (professionActivityIds != null) {
            // In-Memory nach Beruf filtern: die Knoten-ID-Liste wäre als in.(...) zu lang für die URL.
            final Set<UUID> ids = professionActivityIds;
            records = records.stream()
                    .filter(r -> r.curriculum_activity_id() != null && ids.contains(r.curriculum_activity_id()))
                    .toList();
        }

        return records;
    }

    /** Alle Curriculum-Knoten-IDs (Aktivitäten), die zu diesem Bildungsplan gehören. */
    private Set<UUID> getProfessionActivityIds(UUID organizationId, UUID professionId) {
        return supabaseService.getListWithQuery(
                "admin.curriculum_nodes",
                "organization_id=eq." + organizationId + "&profession_id=eq." + professionId,
                CurriculumNodeResponse.class
        ).stream().map(CurriculumNodeResponse::id).collect(Collectors.toSet());
    }

    public List<Profile> getProfessionMembers(UUID organizationId, UUID professionId) {
        var activityIds = getProfessionActivityIds(organizationId, professionId);
        if (activityIds.isEmpty()) return List.of();

        var userIds = supabaseService.getListWithQuery(
                "app.activity_records",
                "organization_id=eq." + organizationId,
                ActivityRecordResponse.class
        ).stream()
                .filter(r -> r.curriculum_activity_id() != null && activityIds.contains(r.curriculum_activity_id()))
                .map(ActivityRecordResponse::user_id)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) return List.of();

        return organizationDomainService.getOrganizationMemberProfiles(organizationId).stream()
                .filter(p -> userIds.contains(p.id()))
                .toList();
    }

    private Map<UUID, String> buildActivityLabels(List<CurriculumNodeResponse> nodes) {
        var parentIds = nodes.stream()
                .map(CurriculumNodeResponse::parent_id)
                .filter(Objects::nonNull)
                .map(UUID::toString)
                .distinct()
                .collect(Collectors.joining(","));

        Map<UUID, String> parentLabelMap = parentIds.isEmpty() ? Map.of() :
                supabaseService.getListWithQuery(
                        "admin.curriculum_nodes",
                        "id=in.(" + parentIds + ")",
                        CurriculumNodeResponse.class
                ).stream().collect(Collectors.toMap(CurriculumNodeResponse::id, CurriculumNodeResponse::label));

        return nodes.stream().collect(Collectors.toMap(
                CurriculumNodeResponse::id,
                n -> {
                    String parentLabel = n.parent_id() != null ? parentLabelMap.get(n.parent_id()) : null;
                    return parentLabel != null ? parentLabel + " / " + n.label() : n.label();
                }
        ));
    }
}
