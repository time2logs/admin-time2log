package ch.time2log.backend.api.rest;

import ch.time2log.backend.api.rest.dto.inbound.AcceptInviteRequest;
import ch.time2log.backend.domain.OnboardingDomainService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/invites")
public class InviteController {

    private final OnboardingDomainService onboardingDomainService;

    public InviteController(OnboardingDomainService onboardingDomainService) {
        this.onboardingDomainService = onboardingDomainService;
    }

    @PostMapping("/accept")
    public void accept(@RequestBody AcceptInviteRequest request) {
        onboardingDomainService.acceptInviteAsExistingUser(request.token());
    }
}
