package ch.time2log.backend.api.rest.dto.outbound;

import java.util.List;

public record DashboardSummaryDto(
        List<ActivitySummaryDto> activities,
        List<LocationSummaryDto> locations,
        List<RatingSummaryDto> ratings
) {}
