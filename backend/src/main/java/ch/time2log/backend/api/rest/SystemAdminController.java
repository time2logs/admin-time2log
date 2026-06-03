package ch.time2log.backend.api.rest;

import ch.time2log.backend.api.rest.dto.inbound.CreatePlatformInviteRequest;
import ch.time2log.backend.api.rest.dto.outbound.InviteDto;
import ch.time2log.backend.api.rest.dto.outbound.ProfileDto;
import ch.time2log.backend.domain.SystemAdminDomainService;
import ch.time2log.backend.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/system-admin")
public class SystemAdminController {

    private final SystemAdminDomainService systemAdminDomainService;

    public SystemAdminController(SystemAdminDomainService systemAdminDomainService) {
        this.systemAdminDomainService = systemAdminDomainService;
    }

    @PostMapping("/invites")
    @ResponseStatus(HttpStatus.CREATED)
    public InviteDto createPlatformInvite(
            @RequestBody CreatePlatformInviteRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return InviteDto.of(systemAdminDomainService.createPlatformInvite(request.email(), user.id()));
    }

    @GetMapping("/admins")
    public List<ProfileDto> getAdmins() {
        return ProfileDto.ofList(systemAdminDomainService.getAdmins());
    }

    @GetMapping("/invites/{userRole}")
    public List<InviteDto> listInvites(@PathVariable String userRole) {
        return InviteDto.ofList(systemAdminDomainService.listInvites(userRole));
    }

    @DeleteMapping("/invites/{inviteId}")
    public void deleteInvite(@PathVariable UUID inviteId) {
        systemAdminDomainService.deleteInvite(inviteId);
    }
}
