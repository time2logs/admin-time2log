package ch.time2log.backend.api.rest;

import ch.time2log.backend.api.rest.dto.inbound.AddModeratorLearnerRequest;
import ch.time2log.backend.api.rest.dto.outbound.ProfileDto;
import ch.time2log.backend.domain.ModeratorDomainService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("api/organizations/{organizationId}/moderators/{moderatorId}/learners")
@RestController
public class ModeratorsController {

    private final ModeratorDomainService moderatorDomainService;

    public ModeratorsController(ModeratorDomainService moderatorDomainService) {
        this.moderatorDomainService = moderatorDomainService;
    }

    @GetMapping
    public List<ProfileDto> getAssignedLearners(@PathVariable UUID organizationId, @PathVariable UUID moderatorId) {
        return ProfileDto.ofList(moderatorDomainService.getAssignedLearners(organizationId, moderatorId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void assignLearner(@PathVariable UUID organizationId, @PathVariable UUID moderatorId, @RequestBody AddModeratorLearnerRequest request) {
        moderatorDomainService.assignLearner(organizationId, moderatorId, request.learnerId());
    }

    @DeleteMapping("/{learnerId}")
    public void unassignLearner(@PathVariable UUID organizationId, @PathVariable UUID moderatorId, @PathVariable UUID learnerId) {
        moderatorDomainService.unassignLearner(organizationId, moderatorId, learnerId);
    }
}
