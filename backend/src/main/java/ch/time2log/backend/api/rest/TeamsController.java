package ch.time2log.backend.api.rest;

import ch.time2log.backend.api.rest.dto.inbound.AddTeamMemberRequest;
import ch.time2log.backend.api.rest.dto.inbound.CreateTeamRequest;
import ch.time2log.backend.api.rest.dto.outbound.ProfileDto;
import ch.time2log.backend.api.rest.dto.outbound.TeamDto;
import ch.time2log.backend.api.rest.exception.EntityAlreadyExistsException;
import ch.time2log.backend.api.rest.exception.EntityNotCreatedException;
import ch.time2log.backend.api.rest.exception.NoRowsAffectedException;
import ch.time2log.backend.infrastructure.supabase.SupabaseApiException;
import ch.time2log.backend.infrastructure.supabase.SupabaseService;
import ch.time2log.backend.infrastructure.supabase.responses.ProfileResponse;
import ch.time2log.backend.infrastructure.supabase.responses.TeamMemberResponse;
import ch.time2log.backend.infrastructure.supabase.responses.TeamResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RequestMapping("api/organizations/{organizationId}/teams")
@RestController
public class TeamsController {

    private final SupabaseService supabase;

    public TeamsController(SupabaseService supabase) {
        this.supabase = supabase;
    }

    @GetMapping
    public List<TeamDto> getTeams(@PathVariable String organizationId) {
        var teams = supabase.getListWithQuery(
                "admin.teams",
                "organization_id=eq." + organizationId,
                TeamResponse.class
        );
        return TeamDto.ofList(teams);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeamDto createTeam(
            @PathVariable String organizationId,
            @RequestBody CreateTeamRequest request) {
        var body = Map.of(
                "organization_id", organizationId,
                "profession_id", request.professionId(),
                "name", request.name()
        );
        var created = supabase.post("admin.teams", body, TeamResponse[].class);
        if (created == null || created.length == 0) {
            throw new EntityNotCreatedException("Team could not be created");
        }
        return TeamDto.of(created[0]);
    }

    @DeleteMapping("/{teamId}")
    public void deleteTeam(
            @PathVariable String organizationId,
            @PathVariable String teamId) {
        int deleted = supabase.deleteReturningCount(
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

    @GetMapping("/{teamId}/members")
    public List<ProfileDto> getTeamMembers(
            @PathVariable String organizationId,
            @PathVariable String teamId) {
        var members = supabase.getListWithQuery(
                "admin.team_members",
                "team_id=eq." + teamId,
                TeamMemberResponse.class
        );
        if (members.isEmpty()) {
            return List.of();
        }

        var memberIds = members.stream()
                .map(TeamMemberResponse::user_id)
                .map(UUID::toString)
                .collect(Collectors.joining(","));

        var profiles = supabase.getListWithQuery(
                "app.profiles",
                "id=in.(" + memberIds + ")",
                ProfileResponse.class
        );
        return ProfileDto.ofList(profiles);
    }

    @PostMapping("/{teamId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public void addTeamMember(
            @PathVariable String organizationId,
            @PathVariable String teamId,
            @RequestBody AddTeamMemberRequest request) {
        var body = Map.of(
                "team_id", teamId,
                "user_id", request.userId(),
                "team_role", request.teamRole()
        );
        try {
            supabase.post("admin.team_members", body, Void.class);
        } catch (SupabaseApiException e) {
            if (e.getStatusCode() == 409) {
                throw new EntityAlreadyExistsException("Member already exists in this team");
            }
            throw e;
        }
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    public void removeTeamMember(
            @PathVariable String organizationId,
            @PathVariable String teamId,
            @PathVariable String userId) {
        int deleted = supabase.deleteReturningCount(
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
