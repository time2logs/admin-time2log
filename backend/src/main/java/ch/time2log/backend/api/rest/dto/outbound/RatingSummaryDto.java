package ch.time2log.backend.api.rest.dto.outbound;

import java.util.List;

public record RatingSummaryDto(
        String activityId,
        String activityName,
        double averageRating
) {
    public static List<RatingSummaryDto> ofList(List<ch.time2log.backend.domain.models.RatingSummary> list) {
        return list.stream()
                .map(r -> new RatingSummaryDto(r.activityId().toString(), r.activityName(), r.averageRating()))
                .toList();
    }
}