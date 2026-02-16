package ch.time2log.backend.api.rest.dto.outbound;

import ch.time2log.backend.infrastructure.supabase.responses.ProfileResponse;

import java.util.List;
import java.util.UUID;

public record ProfileDto(
        UUID id,
        String firstName,
        String lastName
) {
    public static ProfileDto of(ProfileResponse profileResponse) {
        return new ProfileDto(profileResponse.id(), profileResponse.first_name(), profileResponse.last_name());
    }

    public static List<ProfileDto> ofList(List<ProfileResponse> profileResponses) {
        return profileResponses.stream().map(ProfileDto::of).toList();
    }
}
