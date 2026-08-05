package ch.time2log.backend.api.rest.dto.outbound;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public record LocationSummaryDto(
        String location,
        BigDecimal totalHours
) {
    public static List<LocationSummaryDto> ofMap(Map<String, BigDecimal> map) {
        return map.entrySet().stream()
                .map(e -> new LocationSummaryDto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(LocationSummaryDto::totalHours).reversed())
                .toList();
    }
}
