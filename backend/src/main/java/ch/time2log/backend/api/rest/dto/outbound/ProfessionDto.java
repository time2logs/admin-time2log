package ch.time2log.backend.api.rest.dto.outbound;

import ch.time2log.backend.infrastructure.supabase.responses.ProfessionResponse;

import java.util.List;
import java.util.UUID;

public record ProfessionDto(
        UUID id,
        String key,
        String label
) {
    public static ProfessionDto of(ProfessionResponse response) {
        return new ProfessionDto(response.id(), response.key(), response.label());
    }

    public static List<ProfessionDto> ofList(List<ProfessionResponse> responses) {
        return responses.stream().map(ProfessionDto::of).toList();
    }
}
