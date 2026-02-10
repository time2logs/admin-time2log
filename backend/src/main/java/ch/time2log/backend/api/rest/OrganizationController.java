package ch.time2log.backend.api.rest;

import ch.time2log.backend.api.rest.dto.OrganizationDto;
import ch.time2log.backend.api.rest.dto.ProfileDto;
import ch.time2log.backend.persistence.organization.OrganizationEntity;
import ch.time2log.backend.persistence.organization.OrganizationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("api/organizations")
@RestController
public class OrganizationController {
    private final OrganizationRepository organizationRepository;

    public OrganizationController(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @GetMapping
    public List<OrganizationDto> getOrganizations() {
        List<OrganizationEntity> entities = this.organizationRepository.findAll();
        return OrganizationDto.ofList(entities);
    }

    @GetMapping("/{id}/members")
    public List<ProfileDto> getOrganizationMembers() {
        return List.of();
    }

    @PostMapping
    public OrganizationDto createOrganization(String name) {
        OrganizationEntity entity = new OrganizationEntity(name);
        this.organizationRepository.save(entity);
        return OrganizationDto.of(entity);
    }
}