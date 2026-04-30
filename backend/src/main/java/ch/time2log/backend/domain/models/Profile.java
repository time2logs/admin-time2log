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
        OffsetDateTime updatedAt,
        String role,
        String colorblindType
) {
    public static Profile of(ProfileResponse profileResponse) {
        return new Profile(
                profileResponse.id(),
                profileResponse.first_name(),
                profileResponse.last_name(),
                profileResponse.created_at(),
                profileResponse.updated_at(),
                null,
                profileResponse.colorblind_type()
        );
    }

    public Profile withRole(String role) {
        return new Profile(id, firstName, lastName, createdAt, updatedAt, role, colorblind);
    }

    public static List<Profile> ofList(List<ProfileResponse> profiles) {
        return profiles.stream().map(Profile::of).toList();
    }
}