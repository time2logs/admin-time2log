package ch.time2log.backend.api.rest;

import ch.time2log.backend.api.rest.dto.inbound.CreateCurriculumImportRequest;
import ch.time2log.backend.api.rest.dto.outbound.CurriculumImportDto;
import ch.time2log.backend.api.rest.exception.EntityNotCreatedException;
import ch.time2log.backend.api.rest.exception.EntityNotFoundException;
import ch.time2log.backend.infrastructure.supabase.SupabaseService;
import ch.time2log.backend.infrastructure.supabase.responses.CurriculumImportResponse;
import ch.time2log.backend.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequestMapping("api/organizations/{organizationId}/professions/{professionId}/curriculum-imports")
@RestController
public class CurriculumImportController {

    private final SupabaseService supabase;

    public CurriculumImportController(SupabaseService supabase) {
        this.supabase = supabase;
    }

    @GetMapping
    public List<CurriculumImportDto> getImports(
            @PathVariable String organizationId,
            @PathVariable String professionId) {
        var imports = supabase.getListWithQuery(
                "admin.curriculum_imports",
                "organization_id=eq." + organizationId + "&profession_id=eq." + professionId + "&order=created_at.desc",
                CurriculumImportResponse.class
        );
        return imports.stream().map(CurriculumImportDto::of).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CurriculumImportDto createImport(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String organizationId,
            @PathVariable String professionId,
            @RequestBody CreateCurriculumImportRequest request) {
        var body = Map.of(
                "organization_id", organizationId,
                "profession_id", professionId,
                "uploaded_by", user.id(),
                "payload", request,
                "version", request.version()
        );
        var created = supabase.post("admin.curriculum_imports", body, CurriculumImportResponse[].class);
        if (created == null || created.length == 0) {
            throw new EntityNotCreatedException("Curriculum import could not be created");
        }
        return CurriculumImportDto.of(created[0]);
    }

    @PostMapping("/{importId}/apply")
    public CurriculumImportDto applyImport(
            @PathVariable String organizationId,
            @PathVariable String professionId,
            @PathVariable String importId) {
        supabase.rpc("admin.apply_curriculum_import", Map.of("import_id", importId), String.class);

        var imports = supabase.getListWithQuery(
                "admin.curriculum_imports",
                "id=eq." + importId,
                CurriculumImportResponse.class
        );
        if (imports.isEmpty()) {
            throw new EntityNotFoundException("Curriculum import not found");
        }
        return CurriculumImportDto.of(imports.getFirst());
    }
}
