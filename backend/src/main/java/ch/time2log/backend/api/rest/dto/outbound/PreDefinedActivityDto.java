package ch.time2log.backend.api.rest.dto.outbound;

import ch.time2log.backend.infrastructure.supabase.responses.PreDefinedActivityResponse;

import java.util.List;
import java.util.UUID;

public record PreDefinedActivityDto(
        UUID id,
        String key,
        String label,
        String description,
        String category,
        boolean isActive
) {
    public static PreDefinedActivityDto of(PreDefinedActivityResponse response) {
        return new PreDefinedActivityDto(
                response.id(),
                response.key(),
                response.label(),
                response.description(),
                response.category(),
                response.is_active()
        );
    }

    public static List<PreDefinedActivityDto> ofList(List<PreDefinedActivityResponse> responses) {
        return responses.stream().map(PreDefinedActivityDto::of).toList();
    }
}
