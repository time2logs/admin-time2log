package ch.time2log.backend.api.rest.dto.outbound;

import java.util.List;
import java.util.Map;

public record LocationSummaryDto(
        String location,
        int totalHours
) {
    public static List<LocationSummaryDto> ofMap(Map<String, Integer> map) {
        return map.entrySet().stream()
                .map(e -> new LocationSummaryDto(e.getKey(), e.getValue()))
                .sorted((a, b) -> Integer.compare(b.totalHours(), a.totalHours()))
                .toList();
    }
}
