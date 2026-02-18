package ch.time2log.backend.api.rest.dto.outbound;

import ch.time2log.backend.infrastructure.supabase.responses.TeamResponse;

import java.util.List;
import java.util.UUID;

public record TeamDto(
        UUID id,
        UUID professionId,
        String name
) {
    public static TeamDto of(TeamResponse response) {
        return new TeamDto(response.id(), response.profession_id(), response.name());
    }

    public static List<TeamDto> ofList(List<TeamResponse> responses) {
        return responses.stream().map(TeamDto::of).toList();
    }
}
