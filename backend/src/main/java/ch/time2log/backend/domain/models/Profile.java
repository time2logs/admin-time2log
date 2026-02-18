package ch.time2log.backend.domain.models;

import ch.time2log.backend.infrastructure.supabase.responses.ProfileResponse;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record Profile(
        UUID id,
        String firstName,
        String lastName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static Profile of(ProfileResponse profileResponse) {
        return new Profile(
                profileResponse.id(),
                profileResponse.first_name(),
                profileResponse.last_name(),
                profileResponse.created_at(),
                profileResponse.updated_at()
        );
    }

    public static List<Profile> ofList(List<ProfileResponse> profiles) {
        return profiles.stream().map(Profile::of).toList();
    }
}
