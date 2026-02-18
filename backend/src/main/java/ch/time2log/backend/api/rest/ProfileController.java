package ch.time2log.backend.api.rest;

import ch.time2log.backend.api.rest.dto.outbound.ProfileDto;
import ch.time2log.backend.domain.ProfileDomainService;
import ch.time2log.backend.infrastructure.supabase.SupabaseAdminClient;
import ch.time2log.backend.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/profile")
public class ProfileController {

    private final ProfileDomainService profileDomainService;
    private final SupabaseAdminClient supabaseAdmin;

    public ProfileController(ProfileDomainService profileDomainService, SupabaseAdminClient supabaseAdmin) {
        this.profileDomainService = profileDomainService;
        this.supabaseAdmin = supabaseAdmin;
    }

    @GetMapping
    public ProfileDto getProfile(@AuthenticationPrincipal AuthenticatedUser user) {
        return ProfileDto.of(profileDomainService.getById(user.id()));
    }

    @GetMapping("/{id}")
    public ProfileDto getProfileById(@PathVariable UUID id) {
        return ProfileDto.of(profileDomainService.getById(id));
    }

    @DeleteMapping
    public void deleteProfile(@AuthenticationPrincipal AuthenticatedUser user) {
        supabaseAdmin.deleteUser(user.id()).block();
    }
}
