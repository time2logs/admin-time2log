package ch.time2log.backend.api.rest.dto.outbound;

import ch.time2log.backend.domain.models.ActivitySummary;

import java.util.List;
import java.util.UUID;

public record ActivitySummaryDto(
        UUID activityId,
        String activityName,
        int totalHours
) {
    public static ActivitySummaryDto of(ActivitySummary a) {
        return new ActivitySummaryDto(a.activityId(), a.activityName(), a.totalHours());
    }

    public static List<ActivitySummaryDto> ofList(List<ActivitySummary> list) {
        return list.stream().map(ActivitySummaryDto::of).toList();
    }
}
