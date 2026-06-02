package ch.time2log.backend.api.rest;

import ch.time2log.backend.api.rest.dto.inbound.CompleteOnboardingRequest;
import ch.time2log.backend.api.rest.dto.outbound.OnboardingInviteDto;
import ch.time2log.backend.domain.OnboardingDomainService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public (unauthenticated) endpoints for the invite-driven admin onboarding.
 * Permitted in SecurityConfig under /api/onboarding/**.
 */
@RestController
@RequestMapping("api/onboarding")
public class OnboardingController {

    private final OnboardingDomainService onboardingDomainService;

    public OnboardingController(OnboardingDomainService onboardingDomainService) {
        this.onboardingDomainService = onboardingDomainService;
    }

    @GetMapping("/invite")
    public OnboardingInviteDto getInvite(@RequestParam("token") String token) {
        var details = onboardingDomainService.getInviteDetails(token);
        return new OnboardingInviteDto(details.organization_name(), details.email(), details.role());
    }

    @PostMapping("/complete")
    public void complete(@RequestBody CompleteOnboardingRequest request) {
        onboardingDomainService.complete(
                request.token(),
                request.firstName(),
                request.lastName(),
                request.password()
        );
    }
}
