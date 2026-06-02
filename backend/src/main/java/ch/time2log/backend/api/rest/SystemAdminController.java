package ch.time2log.backend.api.rest;

import ch.time2log.backend.api.rest.dto.inbound.CreatePlatformInviteRequest;
import ch.time2log.backend.api.rest.dto.outbound.InviteDto;
import ch.time2log.backend.api.rest.dto.outbound.ProfileDto;
import ch.time2log.backend.domain.SystemAdminDomainService;
import ch.time2log.backend.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
