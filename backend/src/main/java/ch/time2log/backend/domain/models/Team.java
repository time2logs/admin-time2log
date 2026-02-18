package ch.time2log.backend.domain.models;

import ch.time2log.backend.infrastructure.supabase.responses.TeamResponse;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record Team(
        UUID id,
        UUID organization_id,
        UUID profession_id,
        String name,
        UUID created_by,
        OffsetDateTime created_at,
        OffsetDateTime updated_at
) {
        public static Team of(TeamResponse response) {
            return new Team(
                    response.id(),
                    response.organization_id(),
                    response.profession_id(),
                    response.name(),
                    response.created_by(),
                    response.created_at(),
                    response.updated_at()
            );
        }

        public static List<Team> ofList(List<TeamResponse> responses) {
            return responses.stream().map(Team::of).toList();
        }
}
