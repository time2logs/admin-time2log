package ch.time2log.backend.api.rest;

import ch.time2log.backend.api.rest.dto.inbound.AddTeamMemberRequest;
import ch.time2log.backend.api.rest.dto.inbound.CreateTeamRequest;
import ch.time2log.backend.api.rest.dto.outbound.ProfileDto;
import ch.time2log.backend.api.rest.dto.outbound.TeamDto;
import ch.time2log.backend.domain.TeamDomainService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("api/organizations/{organizationId}/teams")
@RestController
public class TeamsController {

    private final TeamDomainService teamDomainService;

    public TeamsController(TeamDomainService teamDomainService) {
        this.teamDomainService = teamDomainService;
    }

    @GetMapping
    public List<TeamDto> getTeams(@PathVariable UUID organizationId) {
        return TeamDto.ofList(teamDomainService.getTeamsByOrganizationId(organizationId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeamDto createTeam(@PathVariable UUID organizationId, @RequestBody CreateTeamRequest request) {
        return TeamDto.of(teamDomainService.createTeam(organizationId, request.professionId(), request.name()));
    }

    @DeleteMapping("/{teamId}")
    public void deleteTeam(@PathVariable UUID organizationId, @PathVariable UUID teamId) {
        teamDomainService.deleteTeam(organizationId, teamId);
    }

    @GetMapping("/{teamId}/members")
    public List<ProfileDto> getTeamMembers(@PathVariable UUID teamId, @PathVariable UUID organizationId) {
        return ProfileDto.ofList(teamDomainService.getTeamMembers(teamId));
    }

    @PostMapping("/{teamId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public void addTeamMember(@PathVariable UUID organizationId, @PathVariable UUID teamId, @RequestBody AddTeamMemberRequest request) {
        teamDomainService.addTeamMember(teamId, request.userId(), request.teamRole());
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    public void removeTeamMember(@PathVariable UUID organizationId, @PathVariable UUID teamId, @PathVariable UUID userId) {
        teamDomainService.removeTeamMember(teamId, userId);
    }
}
