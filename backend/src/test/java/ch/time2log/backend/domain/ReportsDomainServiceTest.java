package ch.time2log.backend.domain;

import ch.time2log.backend.domain.models.DailyMemberReport;
import ch.time2log.backend.domain.models.Profile;
import ch.time2log.backend.infrastructure.supabase.SupabaseService;
import ch.time2log.backend.infrastructure.supabase.responses.ActivityRecordResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
        Profile profile = new Profile(userId, "John", "Doe", OffsetDateTime.now(), OffsetDateTime.now());
        when(organizationDomainService.getOrganizationMemberProfiles(orgId))
                .thenReturn(List.of(profile));
    }

    @Test
    void getDailyReport_whenNoActivityRecords_statusIsMissing() {
        when(supabaseService.getListWithQuery(eq("app.activity_records"), anyString(), eq(ActivityRecordResponse.class)))
                .thenReturn(List.of());

        List<DailyMemberReport> result = reportsDomainService.getDailyReport(orgId, date);

        assertThat(result).hasSize(1);
        DailyMemberReport report = result.getFirst();
        assertThat(report.status()).isEqualTo("missing");
        assertThat(report.totalHours()).isZero();
        assertThat(report.recordCount()).isZero();
        assertThat(report.minRating()).isNull();
    }

    @Test
    void getDailyReport_whenRecordsWithRatingsAboveOne_statusIsReported() {
        when(supabaseService.getListWithQuery(eq("app.activity_records"), anyString(), eq(ActivityRecordResponse.class)))
                .thenReturn(List.of(record(userId, 4, 3), record(userId, 2, 4)));

        DailyMemberReport report = reportsDomainService.getDailyReport(orgId, date).getFirst();

        assertThat(report.status()).isEqualTo("reported");
        assertThat(report.totalHours()).isEqualTo(6);
        assertThat(report.recordCount()).isEqualTo(2);
        assertThat(report.minRating()).isEqualTo(3);
    }

    @Test
    void getDailyReport_whenAnyRatingIsOne_statusIsBadRating() {
        when(supabaseService.getListWithQuery(eq("app.activity_records"), anyString(), eq(ActivityRecordResponse.class)))
                .thenReturn(List.of(record(userId, 3, 4), record(userId, 2, 1)));

        assertThat(reportsDomainService.getDailyReport(orgId, date).getFirst().status())
                .isEqualTo("bad_rating");
    }

    @Test
    void getDailyReport_whenAnyRatingIsZero_statusIsBadRating() {
        when(supabaseService.getListWithQuery(eq("app.activity_records"), anyString(), eq(ActivityRecordResponse.class)))
                .thenReturn(List.of(record(userId, 2, 0)));

        assertThat(reportsDomainService.getDailyReport(orgId, date).getFirst().status())
                .isEqualTo("bad_rating");
    }

    @Test
    void getDailyReport_whenRecordsHaveNoRating_statusIsReported() {
        when(supabaseService.getListWithQuery(eq("app.activity_records"), anyString(), eq(ActivityRecordResponse.class)))
                .thenReturn(List.of(recordNoRating(userId, 4)));

        DailyMemberReport report = reportsDomainService.getDailyReport(orgId, date).getFirst();
        assertThat(report.status()).isEqualTo("reported");
        assertThat(report.minRating()).isNull();
    }

    @Test
    void getDailyReport_recordsOfOtherUserDoNotAffectStatus() {
        UUID otherId = UUID.randomUUID();
        when(supabaseService.getListWithQuery(eq("app.activity_records"), anyString(), eq(ActivityRecordResponse.class)))
                .thenReturn(List.of(record(otherId, 4, 3)));

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

    private ActivityRecordResponse record(UUID uid, int hours, int rating) {
        return new ActivityRecordResponse(UUID.randomUUID(), orgId, uid, null, null, date, hours, null, rating, null, null);
    }

    private ActivityRecordResponse recordNoRating(UUID uid, int hours) {
        return new ActivityRecordResponse(UUID.randomUUID(), orgId, uid, null, null, date, hours, null, null, null, null);
    }
}
