package ch.time2log.backend.domain;

import ch.time2log.backend.domain.exception.EntityNotFoundException;
import ch.time2log.backend.domain.models.Profile;
import ch.time2log.backend.infrastructure.supabase.SupabaseService;
import ch.time2log.backend.infrastructure.supabase.responses.ProfileResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProfileDomainService {

    private final SupabaseService supabaseService;

    public ProfileDomainService(SupabaseService supabaseService) {
        this.supabaseService = supabaseService;
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
}
