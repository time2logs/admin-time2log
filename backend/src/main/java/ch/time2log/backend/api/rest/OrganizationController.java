package ch.time2log.backend.api.rest;

import ch.time2log.backend.api.rest.dto.inbound.CreateOrganizationRequest;
import ch.time2log.backend.api.rest.dto.inbound.InviteRequest;
import ch.time2log.backend.api.rest.dto.outbound.ProfileDto;
import ch.time2log.backend.api.rest.exception.NoRowsAffectedException;
import ch.time2log.backend.infrastructure.supabase.responses.OrganizationMemberResponse;
import ch.time2log.backend.infrastructure.supabase.SupabaseService;
import ch.time2log.backend.infrastructure.supabase.responses.OrganizationResponse;
import ch.time2log.backend.infrastructure.supabase.responses.ProfileResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RequestMapping("api/organizations")
@RestController
public class OrganizationController {

    private final SupabaseService supabase;

    public OrganizationController(SupabaseService supabase) {
        this.supabase = supabase;
    }

    @GetMapping
    public List<OrganizationResponse> getOrganizations() {
        return supabase.getList("admin.organizations", OrganizationResponse.class);
    }

    @PostMapping
    public OrganizationResponse createOrganization(@RequestBody CreateOrganizationRequest request) {
        var body = Map.of("name", request.name());
        var created = supabase.post("admin.organizations", body, OrganizationResponse[].class);
        if (created == null || created.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Supabase returned no created organization");
        }
        return created[0];
    }

    @DeleteMapping("/{id}")
    public void deleteOrganization(@PathVariable UUID id) {
        int deleted = supabase.deleteReturningCount("admin.organizations", "id=eq." + id);
        if (deleted == 0) {
            throw new NoRowsAffectedException(
                    HttpStatus.FORBIDDEN,
                    "ORGANIZATION_DELETE_BLOCKED",
                    "Forbidden operation",
                    "Organization could not be deleted."
            );
        }
    }

    @GetMapping("/{id}/members")
    public List<ProfileDto> getOrganizationMembers(@PathVariable UUID id) {
        var members = supabase.getListWithQuery(
                "admin.organization_members",
                "organization_id=eq." + id,
                OrganizationMemberResponse.class
        );
        if (members.isEmpty()) {
            return List.of();
        }

        var memberIds = members.stream()
                .map(OrganizationMemberResponse::user_id)
                .map(UUID::toString)
                .collect(Collectors.joining(","));

        var profiles = supabase.getListWithQuery(
                "app.profiles",
                "id=in.(" + memberIds + ")",
                ProfileResponse.class
        );

        return ProfileDto.ofList(profiles);
    }

    //Inviting per id later maybe per mail -> todo: PO
    @PostMapping("/{id}/invite")
    public void inviteToOrganization(@PathVariable String id, @RequestBody InviteRequest request) {
        var userId = request.userId();
        var userRole = request.userRole();
        var body = Map.of(
                "user_id", userId,
                "organization_id", id,
                "user_role", userRole
        );
        supabase.post("admin.organization_members", body, Void.class);
    }

    @DeleteMapping("/{id}/members/{userId}")
    public void removeOrganizationMember(@PathVariable String id, @PathVariable String userId) {
        int deleted = supabase.deleteReturningCount("admin.organization_members", "organization_id=eq." + id + "&user_id=eq." + userId);
        if (deleted == 0) {
            throw new NoRowsAffectedException(
                    HttpStatus.FORBIDDEN,
                    "MEMBER_REMOVE_BLOCKED",
                    "Forbidden operation",
                    "Organization member could not be removed."
            );
        }
    }
}
