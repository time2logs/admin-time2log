package ch.time2log.backend.api.rest.dto;

import ch.time2log.backend.persistence.profile.ProfileEntity;

import java.util.UUID;

public record ProfileDto(
        UUID id,
        String firstName,
        String lastName
) {
    public static ProfileDto of(ProfileEntity entity) {
        return new ProfileDto(entity.getId(), entity.getFirstName(), entity.getLastName());
    }
}
