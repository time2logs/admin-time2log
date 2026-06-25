package ch.time2log.backend.api.rest.dto.inbound;

public record CompleteOnboardingRequest(
        String token,
        String firstName,
        String lastName,
        String password
) {}
