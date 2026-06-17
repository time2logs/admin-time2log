package ch.time2log.backend.domain;

import ch.time2log.backend.infrastructure.supabase.SupabaseAdminClient;
import ch.time2log.backend.infrastructure.supabase.responses.OrganizationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

@Component
public class SemesterRolloverService {
    private static final Logger log = LoggerFactory.getLogger(SemesterRolloverService.class);
    private final SupabaseAdminClient adminClient;

    public SemesterRolloverService(SupabaseAdminClient adminClient) {
        this.adminClient = adminClient;
    }

    // täglich um 01:00 Europe/Zurich
    @Scheduled(cron = "0 0 1 * * *", zone = "Europe/Zurich")
    public void rolloverDueSemesters() {
        var today = LocalDate.now(ZoneId.of("Europe/Zurich")).toString();
        var orgs = adminClient.getListWithQuery(
                "admin.organizations",
                "semester_end_date=lte." + today + "&select=id,name",
                OrganizationResponse.class
        );
        for (var org : orgs) {
            try {
                adminClient.callRpc("admin", "rollover_semester",
                        Map.of("p_org_id", org.id()), Void.class);
                log.info("Semester rollover done for org {} ({})", org.id(), org.name());
            } catch (Exception e) {
                log.error("Rollover failed for org {}: {}", org.id(), e.getMessage());
            }
        }
    }
}
