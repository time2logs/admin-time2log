package ch.time2log.backend.api.rest.dto.outbound;

import ch.time2log.backend.domain.models.Team;
import ch.time2log.backend.infrastructure.supabase.responses.TeamResponse;

import java.util.List;
import java.util.UUID;

public record TeamDto(
        UUID id,
        UUID professionId,
        String name
) {
    public static TeamDto of(Team team) {
        return new TeamDto(
                team.id(),
                team.profession_id(),
                team.name()
        );
    }

    public static List<TeamDto> ofList(List<Team> teams) {
        return teams.stream().map(TeamDto::of).toList();
    }
}
