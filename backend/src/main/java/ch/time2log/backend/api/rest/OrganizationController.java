package ch.time2log.backend.api.rest;

import ch.time2log.backend.api.rest.dto.inbound.CreateOrganizationRequest;
import ch.time2log.backend.infrastructure.supabase.SupabaseService;
import ch.time2log.backend.infrastructure.supabase.responses.OrganizationResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

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
}