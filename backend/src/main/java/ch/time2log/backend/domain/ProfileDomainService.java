package ch.time2log.backend.domain;

import ch.time2log.backend.domain.exception.EntityNotFoundException;
import ch.time2log.backend.domain.models.Profile;
import ch.time2log.backend.infrastructure.supabase.SupabaseAdminClient;
import ch.time2log.backend.infrastructure.supabase.SupabaseService;
import ch.time2log.backend.infrastructure.supabase.responses.ProfileResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProfileDomainService {

    private final SupabaseService supabaseService;
    private final SupabaseAdminClient supabaseAdminClient;

    public ProfileDomainService(SupabaseService supabaseService, SupabaseAdminClient supabaseAdminClient) {
        this.supabaseService = supabaseService;
        this.supabaseAdminClient = supabaseAdminClient;
    }

    public boolean existsByEmail(String email) {
        if (email == null || email.isBlank()) return false;
        UUID userId = supabaseAdminClient.getUserIdByEmail(email);
        if (userId == null) return false;
        var profiles = supabaseAdminClient.getListWithQuery(
                "app.profiles",
                "id=eq." + userId + "&select=id&limit=1",
                ProfileResponse.class
        );
        return profiles != null && !profiles.isEmpty();
    }

    public Profile getById(UUID id) {
        var profiles = supabaseService.getListWithQuery("app.profiles", "id=eq." + id, ProfileResponse.class);
        if (profiles.isEmpty()) {
            throw new EntityNotFoundException("Profile not found");
        }
        return Profile.of(profiles.getFirst());
    }

    public List<Profile> getByIds(List<UUID> profileIds) {
        if (profileIds.isEmpty()) return List.of();
        var ids = profileIds.stream().map(UUID::toString).collect(Collectors.joining(","));
        var profiles = supabaseService.getListWithQuery(
                "app.profiles",
                "id=in.(" + ids + ")",
                ProfileResponse.class
        );
        return Profile.ofList(profiles);
    }
    public void updateColorblindType(UUID userId, String colorblindType) {
        supabaseService.patch(
                "app.profiles",
                "id=eq." + userId,
                Map.of("colorblind_type", colorblindType),
                ProfileResponse.class
        );
    }
}
