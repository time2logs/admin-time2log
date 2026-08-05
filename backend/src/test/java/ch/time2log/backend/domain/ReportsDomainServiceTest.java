package ch.time2log.backend.domain;

import ch.time2log.backend.domain.models.DailyMemberReport;
import ch.time2log.backend.domain.models.MemberActivityRecord;
import ch.time2log.backend.domain.models.Profile;
import ch.time2log.backend.infrastructure.supabase.SupabaseService;
import ch.time2log.backend.infrastructure.supabase.responses.ActivityRecordResponse;
import ch.time2log.backend.infrastructure.supabase.responses.CurriculumNodeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportsDomainServiceTest {

    @Mock
    private SupabaseService supabaseService;

    @Mock
    private OrganizationDomainService organizationDomainService;

    @InjectMocks
    private ReportsDomainService reportsDomainService;

    private final UUID orgId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final String date = "2024-01-15";

    @BeforeEach
    void setUp() {
        Profile profile = new Profile(userId, "John", "Doe", OffsetDateTime.now(), OffsetDateTime.now(), "admin", "normal");
        lenient().when(organizationDomainService.getOrganizationMemberProfiles(orgId))
                .thenReturn(List.of(profile));
        lenient().when(organizationDomainService.getTargetHours(orgId))
                .thenReturn(8);
    }

    @Test
    void getDailyReport_whenNoActivityRecords_statusIsMissing() {
        when(supabaseService.getListWithQuery(eq("app.activity_records"), anyString(), eq(ActivityRecordResponse.class)))
                .thenReturn(List.of());

        List<DailyMemberReport> result = reportsDomainService.getDailyReport(orgId, date);

        assertThat(result).hasSize(1);
        DailyMemberReport report = result.getFirst();
        assertThat(report.status()).isEqualTo("missing");
        assertThat(report.totalHours()).isEqualTo(BigDecimal.ZERO.setScale(2));
        assertThat(report.recordCount()).isZero();
        assertThat(report.minRating()).isNull();
    }

    @Test
    void getDailyReport_whenRecordsWithRatingsAboveOne_statusIsReported() {
        when(supabaseService.getListWithQuery(eq("app.activity_records"), anyString(), eq(ActivityRecordResponse.class)))
                .thenReturn(List.of(record(userId, BigDecimal.valueOf(5), 3), record(userId, BigDecimal.valueOf(3), 4)));

        DailyMemberReport report = reportsDomainService.getDailyReport(orgId, date).getFirst();

        assertThat(report.status()).isEqualTo("reported");
        assertThat(report.totalHours()).isEqualTo(BigDecimal.valueOf(8).setScale(2));
        assertThat(report.recordCount()).isEqualTo(2);
        assertThat(report.minRating()).isEqualTo(3);
    }

    @Test
    void getDailyReport_whenAnyRatingIsOne_statusIsBadRating() {
        when(organizationDomainService.getTargetHours(orgId)).thenReturn(2);
        when(supabaseService.getListWithQuery(eq("app.activity_records"), anyString(), eq(ActivityRecordResponse.class)))
                .thenReturn(List.of(record(userId, BigDecimal.valueOf(3), 4), record(userId, BigDecimal.valueOf(2), 1)));

        assertThat(reportsDomainService.getDailyReport(orgId, date).getFirst().status())
                .isEqualTo("bad_rating");
    }

    @Test
    void getDailyReport_whenAnyRatingIsZero_statusIsBadRating() {
        when(supabaseService.getListWithQuery(eq("app.activity_records"), anyString(), eq(ActivityRecordResponse.class)))
                .thenReturn(List.of(record(userId, BigDecimal.valueOf(10), 0)));

        assertThat(reportsDomainService.getDailyReport(orgId, date).getFirst().status())
                .isEqualTo("bad_rating");
    }

    @Test
    void getDailyReport_whenAnyRatingIsTwo_statusIsBadRating() {
        when(supabaseService.getListWithQuery(eq("app.activity_records"), anyString(), eq(ActivityRecordResponse.class)))
                .thenReturn(List.of(record(userId, BigDecimal.valueOf(10), 2)));

        assertThat(reportsDomainService.getDailyReport(orgId, date).getFirst().status())
                .isEqualTo("bad_rating");
    }

    @Test
    void getDailyReport_whenRecordsHaveNoRating_statusIsReported() {
        when(supabaseService.getListWithQuery(eq("app.activity_records"), anyString(), eq(ActivityRecordResponse.class)))
                .thenReturn(List.of(recordNoRating(userId, BigDecimal.valueOf(8))));

        DailyMemberReport report = reportsDomainService.getDailyReport(orgId, date).getFirst();
        assertThat(report.status()).isEqualTo("reported");
        assertThat(report.minRating()).isNull();
    }

    @Test
    void getDailyReport_recordsOfOtherUserDoNotAffectStatus() {
        UUID otherId = UUID.randomUUID();
        when(supabaseService.getListWithQuery(eq("app.activity_records"), anyString(), eq(ActivityRecordResponse.class)))
                .thenReturn(List.of(record(otherId, BigDecimal.valueOf(4), 3)));

        DailyMemberReport report = reportsDomainService.getDailyReport(orgId, date).getFirst();
        assertThat(report.status()).isEqualTo("missing");
    }

    @Test
    void getDailyReport_preservesMemberNameFromProfile() {
        when(supabaseService.getListWithQuery(eq("app.activity_records"), anyString(), eq(ActivityRecordResponse.class)))
                .thenReturn(List.of());

        DailyMemberReport report = reportsDomainService.getDailyReport(orgId, date).getFirst();
        assertThat(report.firstName()).isEqualTo("John");
        assertThat(report.lastName()).isEqualTo("Doe");
        assertThat(report.userId()).isEqualTo(userId);
    }

    @Test
    void getMemberRecords_filtersLocationByTrimmedCaseInsensitiveExactMatch() {
        UUID activityA = UUID.randomUUID();
        UUID activityB = UUID.randomUUID();
        when(supabaseService.getListWithQuery(eq("app.activity_records"), anyString(), eq(ActivityRecordResponse.class)))
                .thenReturn(List.of(
                        recordWithLocation(userId, activityA, " Ward A ", BigDecimal.valueOf(2)),
                        recordWithLocation(userId, activityA, "ward a", BigDecimal.valueOf(3)),
                        recordWithLocation(userId, activityB, "Ward B", BigDecimal.valueOf(4))
                ));
        when(supabaseService.getListWithQuery(eq("admin.curriculum_nodes"), anyString(), eq(CurriculumNodeResponse.class)))
                .thenReturn(List.of(node(activityA, "Activity A")));

        List<MemberActivityRecord> result = reportsDomainService.getMemberRecords(orgId, userId, null, "2024-01-01", "2024-01-31", " ward A ");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(MemberActivityRecord::hours).containsExactly(BigDecimal.valueOf(2), BigDecimal.valueOf(3));
        assertThat(result).extracting(MemberActivityRecord::activityLabel).containsExactly("Activity A", "Activity A");
        assertThat(result).extracting(MemberActivityRecord::location).containsExactly(" Ward A ", "ward a");
    }

    @Test
    void getMemberRecords_blankLocationBehavesLikeNoLocationFilter() {
        when(supabaseService.getListWithQuery(eq("app.activity_records"), anyString(), eq(ActivityRecordResponse.class)))
                .thenReturn(List.of(
                        recordWithLocation(userId, null, "Ward A", BigDecimal.valueOf(2)),
                        recordWithLocation(userId, null, "Ward B", BigDecimal.valueOf(4))
                ));

        List<MemberActivityRecord> result = reportsDomainService.getMemberRecords(orgId, userId, null, "2024-01-01", "2024-01-31", "   ");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(MemberActivityRecord::location).containsExactly("Ward A", "Ward B");
    }

    @Test
    void getDailyReport_whenRecordsHaveDecimalHours_totalIsRoundedToTwoDecimals() {
        when(supabaseService.getListWithQuery(eq("app.activity_records"), anyString(), eq(ActivityRecordResponse.class)))
                .thenReturn(List.of(
                        record(userId, BigDecimal.valueOf(4.5), 3),
                        record(userId, BigDecimal.valueOf(4.25), 4)
                ));

        DailyMemberReport report = reportsDomainService.getDailyReport(orgId, date).getFirst();

        assertThat(report.status()).isEqualTo("reported");
        assertThat(report.totalHours()).isEqualTo(BigDecimal.valueOf(8.75).setScale(2));
        assertThat(report.recordCount()).isEqualTo(2);
    }

    @Test
    void getDailyReport_whenHoursBelowTarget_statusIsUnderTarget() {
        when(supabaseService.getListWithQuery(eq("app.activity_records"), anyString(), eq(ActivityRecordResponse.class)))
                .thenReturn(List.of(record(userId, BigDecimal.valueOf(4), 3), record(userId, BigDecimal.valueOf(2), 4)));

        DailyMemberReport report = reportsDomainService.getDailyReport(orgId, date).getFirst();

        assertThat(report.status()).isEqualTo("under_target");
        assertThat(report.totalHours()).isEqualTo(BigDecimal.valueOf(6).setScale(2));
    }

    @Test
    void getDailyReport_whenBadRatingAndUnderTarget_statusIsBadRatingUnderTarget() {
        when(supabaseService.getListWithQuery(eq("app.activity_records"), anyString(), eq(ActivityRecordResponse.class)))
                .thenReturn(List.of(record(userId, BigDecimal.valueOf(3), 4), record(userId, BigDecimal.valueOf(2), 1)));

        DailyMemberReport report = reportsDomainService.getDailyReport(orgId, date).getFirst();

        assertThat(report.status()).isEqualTo("bad_rating_under_target");
        assertThat(report.totalHours()).isEqualTo(BigDecimal.valueOf(5).setScale(2));
    }

    @Test
    void getDailyReport_whenCustomTargetHoursBelowTotal_statusIsReported() {
        when(organizationDomainService.getTargetHours(orgId)).thenReturn(6);
        when(supabaseService.getListWithQuery(eq("app.activity_records"), anyString(), eq(ActivityRecordResponse.class)))
                .thenReturn(List.of(record(userId, BigDecimal.valueOf(4), 3), record(userId, BigDecimal.valueOf(2), 4)));

        DailyMemberReport report = reportsDomainService.getDailyReport(orgId, date).getFirst();

        assertThat(report.status()).isEqualTo("reported");
        assertThat(report.totalHours()).isEqualTo(BigDecimal.valueOf(6).setScale(2));
    }

    private ActivityRecordResponse record(UUID uid, BigDecimal hours, int rating) {
        return new ActivityRecordResponse(UUID.randomUUID(), orgId, uid, null, null, date, hours, null, rating, null, null, null, null);
    }

    private ActivityRecordResponse recordNoRating(UUID uid, BigDecimal hours) {
        return new ActivityRecordResponse(UUID.randomUUID(), orgId, uid, null, null, date, hours, null, null, null, null, null, null);
    }

    private ActivityRecordResponse recordWithLocation(UUID uid, UUID activityId, String location, BigDecimal hours) {
        return new ActivityRecordResponse(UUID.randomUUID(), orgId, uid, null, activityId, date, hours, null, null, location, null, null, null);
    }

    private CurriculumNodeResponse node(UUID activityId, String label) {
        return new CurriculumNodeResponse(activityId, orgId, UUID.randomUUID(), null, "activity", "key", label, null, 0, true);
    }
}
