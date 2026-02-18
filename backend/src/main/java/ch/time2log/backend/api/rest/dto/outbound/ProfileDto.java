package ch.time2log.backend.api.rest.dto.outbound;

import ch.time2log.backend.domain.models.Profile;
import ch.time2log.backend.infrastructure.supabase.responses.ProfileResponse;

import java.util.List;
import java.util.UUID;

public record ProfileDto(
        UUID id,
        String firstName,
        String lastName
) {
    public static ProfileDto of(Profile profile) {
        return new ProfileDto(profile.id(), profile.firstName(), profile.lastName());
    }

    public static List<ProfileDto> ofList(List<Profile> profiles) {
        return profiles.stream().map(ProfileDto::of).toList();
    }
}
