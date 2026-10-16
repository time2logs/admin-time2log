package ch.time2log.backend.api.rest;

import ch.time2log.backend.api.rest.dto.inbound.AddMentorLearnerRequest;
import ch.time2log.backend.api.rest.dto.outbound.ProfileDto;
import ch.time2log.backend.domain.MentorDomainService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("api/organizations/{organizationId}/mentors/{mentorId}/learners")
@RestController
public class MentorsController {

    private final MentorDomainService mentorDomainService;

    public MentorsController(MentorDomainService mentorDomainService) {
        this.mentorDomainService = mentorDomainService;
    }

    @GetMapping
    public List<ProfileDto> getAssignedLearners(@PathVariable UUID organizationId, @PathVariable UUID mentorId) {
        return ProfileDto.ofList(mentorDomainService.getAssignedLearners(organizationId, mentorId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void assignLearner(@PathVariable UUID organizationId, @PathVariable UUID mentorId, @RequestBody AddMentorLearnerRequest request) {
        mentorDomainService.assignLearner(organizationId, mentorId, request.learnerId());
    }

    @DeleteMapping("/{learnerId}")
    public void unassignLearner(@PathVariable UUID organizationId, @PathVariable UUID mentorId, @PathVariable UUID learnerId) {
        mentorDomainService.unassignLearner(organizationId, mentorId, learnerId);
    }
}
