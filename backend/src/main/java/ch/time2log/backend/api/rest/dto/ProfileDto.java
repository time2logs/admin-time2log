package ch.time2log.backend.api.rest.dto;

import java.util.UUID;

public record ProfileDto(
        UUID id,
        String firstName,
        String lastName
) {
}
