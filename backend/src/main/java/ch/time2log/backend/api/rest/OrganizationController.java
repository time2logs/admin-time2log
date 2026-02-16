package ch.time2log.backend.api.rest;

import ch.time2log.backend.api.rest.dto.inbound.CreateOrganizationRequest;
import ch.time2log.backend.api.rest.dto.inbound.InviteRequest;
import ch.time2log.backend.api.rest.dto.outbound.ProfileDto;
import ch.time2log.backend.infrastructure.supabase.responses.OrganizationMemberResponse;
import ch.time2log.backend.infrastructure.supabase.SupabaseService;
import ch.time2log.backend.infrastructure.supabase.responses.OrganizationResponse;
import ch.time2log.backend.infrastructure.supabase.responses.ProfileResponse;
import org.springframework.web.bind.annotation.*;

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
        return supabase.post("admin.organizations", body, OrganizationResponse.class);
    }

    @GetMapping("/{id}/members")
    public List<ProfileDto> getOrganizationMembers(@PathVariable UUID id) {
        var members = supabase.getListWithQuery(
                "admin.organization_members",
                "organization_id=eq." + id,
                OrganizationMemberResponse.class
        );

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
}
