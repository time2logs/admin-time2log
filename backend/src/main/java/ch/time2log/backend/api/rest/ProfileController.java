package ch.time2log.backend.api.rest;

import ch.time2log.backend.api.rest.dto.ProfileDto;
import ch.time2log.backend.api.rest.exception.ProfileNotFoundException;
import ch.time2log.backend.persistence.profile.ProfileEntity;
import ch.time2log.backend.persistence.profile.ProfileRepository;
import ch.time2log.backend.security.AuthenticatedUser;
import ch.time2log.backend.api.rest.exception.ProfileNotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("api/profile")
public class ProfileController {


    private final ProfileRepository profileRepository;

    public ProfileController(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @GetMapping
    public ProfileDto getProfile(@AuthenticationPrincipal AuthenticatedUser user) {
        System.out.println("Fetching profile for user id: " + user.id());
        ProfileEntity entity = this.profileRepository.findById(user.id())
                .orElseThrow(() -> new ProfileNotFoundException ("Profile not found for user id: " + user.id()));

        return ProfileDto.of(entity);
    }

    @GetMapping("/{id}")
    public ProfileDto getProfileById(@PathVariable UUID id) {
        ProfileEntity entity = this.profileRepository.findById(id)
                .orElseThrow(() -> new ProfileNotFoundException ("Profile not found for id: " + id));
        return ProfileDto.of(entity);
    }

}
