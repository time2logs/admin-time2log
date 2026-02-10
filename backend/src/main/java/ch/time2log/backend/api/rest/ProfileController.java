package ch.time2log.backend.api.rest;

import ch.time2log.backend.api.rest.dto.ProfileDto;
import ch.time2log.backend.persistence.profile.ProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.stereotype.Repository;
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
    public ProfileDto getProfile() {
        return new ProfileDto(
                UUID.randomUUID(),
                "John",
                "Doe"
        );
    }

    @GetMapping("/{id}")
    public ProfileDto getProfileById(@PathVariable UUID id) {
        return new ProfileDto(
                UUID.randomUUID(),
                "John",
                "Doe"
        );
    }

}
