package ch.time2log.backend.api.rest;

import ch.time2log.backend.api.rest.dto.inbound.CreateProfessionRequest;
import ch.time2log.backend.api.rest.dto.outbound.ProfessionDto;
import ch.time2log.backend.api.rest.exception.EntityNotCreatedException;
import ch.time2log.backend.infrastructure.supabase.SupabaseService;
import ch.time2log.backend.infrastructure.supabase.responses.ProfessionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequestMapping("api/organizations/{organizationId}/professions")
@RestController
public class ProfessionController {

    private final SupabaseService supabase;

    public ProfessionController(SupabaseService supabase) {
        this.supabase = supabase;
    }

    @GetMapping
    public List<ProfessionDto> getProfessions(@PathVariable String organizationId) {
        var professions = supabase.getListWithQuery(
                "admin.professions",
                "organization_id=eq." + organizationId,
                ProfessionResponse.class
        );
        return ProfessionDto.ofList(professions);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfessionDto createProfession(
            @PathVariable String organizationId,
            @RequestBody CreateProfessionRequest request) {
        var body = Map.of(
                "organization_id", organizationId,
                "key", request.key(),
                "label", request.label()
        );
        var created = supabase.post("admin.professions", body, ProfessionResponse[].class);
        if (created == null || created.length == 0) {
            throw new EntityNotCreatedException("Profession could not be created");
        }
        return ProfessionDto.of(created[0]);
    }
}
