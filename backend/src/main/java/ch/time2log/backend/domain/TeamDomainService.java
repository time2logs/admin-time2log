package ch.time2log.backend.domain;

import ch.time2log.backend.api.rest.exception.EntityAlreadyExistsException;
import ch.time2log.backend.api.rest.exception.EntityNotCreatedException;
import ch.time2log.backend.api.rest.exception.NoRowsAffectedException;
import ch.time2log.backend.domain.models.Profile;
import ch.time2log.backend.domain.models.Team;
import ch.time2log.backend.infrastructure.supabase.SupabaseApiException;
import ch.time2log.backend.infrastructure.supabase.SupabaseService;
import ch.time2log.backend.infrastructure.supabase.responses.TeamMemberResponse;
import ch.time2log.backend.infrastructure.supabase.responses.TeamResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TeamDomainService {
    private final SupabaseService supabaseService;
    private final ProfileDomainService profileDomainService;

    public TeamDomainService(SupabaseService supabaseService, ProfileDomainService profileDomainService) {
        this.supabaseService = supabaseService;
        this.profileDomainService = profileDomainService;
    }

    public List<Team> getTeamsByOrganizationId(UUID organizationId) {
        var teams = supabaseService.getListWithQuery(
                "admin.teams",
                "organization_id=eq." + organizationId,
                TeamResponse.class
        );
        return Team.ofList(teams);
    }

    public Team createTeam(UUID organizationId, UUID professionId, String name) {
        var body = Map.of(
                "organization_id", organizationId,
                "profession_id", professionId,
                "name", name
        );
        var created = supabaseService.post("admin.teams", body, TeamResponse[].class);
        if (created == null || created.length == 0) {
            throw new EntityNotCreatedException("Team could not be created");
        }
        return Team.of(created[0]);
    }

    public void deleteTeam(UUID organizationId, UUID teamId) {
        int deleted = supabaseService.deleteReturningCount(
                "admin.teams",
                "id=eq." + teamId + "&organization_id=eq." + organizationId
        );
        if (deleted == 0) {
            throw new NoRowsAffectedException(
                    HttpStatus.FORBIDDEN,
                    "TEAM_DELETE_BLOCKED",
                    "Forbidden operation",
                    "Team could not be deleted."
            );
        }
    }

    public List<Profile> getTeamMembers(UUID teamId) {
        var members = supabaseService.getListWithQuery(
                "admin.team_members",
                "team_id=eq." + teamId,
                TeamMemberResponse.class
        );

        var ids = members.stream().map(TeamMemberResponse::user_id).toList();
        return profileDomainService.getByIds(ids);
    }

    public void addTeamMember(UUID teamId, UUID userId, String teamRole) {
        var body = Map.of(
                "team_id", teamId,
                "user_id", userId,
                "team_role", teamRole
        );
        try {
            supabaseService.post("admin.team_members", body, Void.class);
        } catch (SupabaseApiException e) {
            if (e.getStatusCode() == 409) {
                throw new EntityAlreadyExistsException("Member already exists in this team");
            }
            throw e;
        }
    }

    public void removeTeamMember(UUID teamId, UUID userId) {
        int deleted = supabaseService.deleteReturningCount(
                "admin.team_members",
                "team_id=eq." + teamId + "&user_id=eq." + userId
        );
        if (deleted == 0) {
            throw new NoRowsAffectedException(
                    HttpStatus.FORBIDDEN,
                    "TEAM_MEMBER_REMOVE_BLOCKED",
                    "Forbidden operation",
                    "Team member could not be removed."
            );
        }
    }
}
