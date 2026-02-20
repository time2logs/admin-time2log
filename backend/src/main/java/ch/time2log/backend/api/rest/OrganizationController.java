package ch.time2log.backend.api.rest;

import ch.time2log.backend.api.rest.dto.inbound.CreateInviteRequest;
import ch.time2log.backend.api.rest.dto.inbound.CreateOrganizationRequest;
import ch.time2log.backend.api.rest.dto.outbound.InviteDto;
import ch.time2log.backend.api.rest.dto.outbound.OrganizationDto;
import ch.time2log.backend.api.rest.dto.outbound.ProfileDto;
import ch.time2log.backend.domain.OrganizationDomainService;
import ch.time2log.backend.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @DeleteMapping("/{id}/members/{userId}")
    public void removeOrganizationMember(@PathVariable UUID id, @PathVariable UUID userId) {
        orgDomainService.removeOrganizationMember(id, userId);
    }

    @PostMapping("/{id}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    public InviteDto createInvite(
            @PathVariable UUID id,
            @RequestBody CreateInviteRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return InviteDto.of(orgDomainService.createInvite(id, request.email(), request.userRole(), user.id()));
    }

    @GetMapping("/{id}/invites")
    public List<InviteDto> listInvites(@PathVariable UUID id) {
        return InviteDto.ofList(orgDomainService.listInvites(id));
    }

    @DeleteMapping("/{id}/invites/{inviteId}")
    public void deleteInvite(@PathVariable UUID id, @PathVariable UUID inviteId) {
        orgDomainService.deleteInvite(id, inviteId);
    }
}
