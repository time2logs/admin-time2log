package ch.time2log.backend.api.rest;

import ch.time2log.backend.api.rest.dto.inbound.CreateOrganizationRequest;
import ch.time2log.backend.api.rest.dto.inbound.InviteRequest;
import ch.time2log.backend.api.rest.dto.outbound.OrganizationDto;
import ch.time2log.backend.api.rest.dto.outbound.ProfileDto;
import ch.time2log.backend.domain.OrganizationDomainService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("api/organizations")
@RestController
public class OrganizationController {

    private final OrganizationDomainService orgDomainService;

    public OrganizationController(OrganizationDomainService orgDomainService) {
        this.orgDomainService = orgDomainService;
    }

    @GetMapping
    public List<OrganizationDto> getOrganizations() {
        return OrganizationDto.ofList(orgDomainService.getOrganizations());
    }

    @PostMapping
    public OrganizationDto createOrganization(@RequestBody CreateOrganizationRequest request) {
        return OrganizationDto.of(orgDomainService.createOrganization(request.name()));
    }

    @DeleteMapping("/{id}")
    public void deleteOrganization(@PathVariable UUID id) {
        orgDomainService.deleteOrganization(id);
    }

    @GetMapping("/{id}/members")
    public List<ProfileDto> getOrganizationMembers(@PathVariable UUID id) {
        return ProfileDto.ofList(orgDomainService.getOrganizationMemberProfiles(id));
    }

    @PostMapping("/{id}/invite")
    public void inviteToOrganization(@PathVariable UUID id, @RequestBody InviteRequest request) {
        orgDomainService.inviteToOrganization(id, request.userId(), request.userRole());
    }

    @DeleteMapping("/{id}/members/{userId}")
    public void removeOrganizationMember(@PathVariable UUID id, @PathVariable UUID userId) {
        orgDomainService.removeOrganizationMember(id, userId);
    }
}
