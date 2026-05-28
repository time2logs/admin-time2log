package ch.time2log.backend.api.rest.dto.outbound;

public record OnboardingInviteDto(
        String organizationName,
        String email,
        String role
) {}
