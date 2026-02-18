package ch.time2log.backend.domain;

import ch.time2log.backend.domain.exception.EntityNotCreatedException;
import ch.time2log.backend.domain.exception.EntityNotFoundException;
import ch.time2log.backend.domain.models.CurriculumImport;
import ch.time2log.backend.infrastructure.supabase.SupabaseService;
import ch.time2log.backend.infrastructure.supabase.responses.CurriculumImportResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CurriculumImportDomainService {

    private final SupabaseService supabaseService;

    public CurriculumImportDomainService(SupabaseService supabaseService) {
        this.supabaseService = supabaseService;
    }

    public List<CurriculumImport> getImports(UUID organizationId, UUID professionId) {
        var responses = supabaseService.getListWithQuery(
                "admin.curriculum_imports",
                "organization_id=eq." + organizationId + "&profession_id=eq." + professionId + "&order=created_at.desc",
                CurriculumImportResponse.class
        );
        return CurriculumImport.ofList(responses);
    }

    public CurriculumImport createImport(UUID organizationId, UUID professionId, UUID uploadedBy, Object payload, String version) {
        var body = Map.of(
                "organization_id", organizationId,
                "profession_id", professionId,
                "uploaded_by", uploadedBy,
                "payload", payload,
                "version", version
        );
        var created = supabaseService.post("admin.curriculum_imports", body, CurriculumImportResponse[].class);
        if (created == null || created.length == 0) {
            throw new EntityNotCreatedException("Curriculum import could not be created");
        }
        return CurriculumImport.of(created[0]);
    }

    public CurriculumImport applyImport(UUID importId) {
        supabaseService.rpc("admin.apply_curriculum_import", Map.of("import_id", importId), String.class);

        var responses = supabaseService.getListWithQuery(
                "admin.curriculum_imports",
                "id=eq." + importId,
                CurriculumImportResponse.class
        );
        if (responses.isEmpty()) {
            throw new EntityNotFoundException("Curriculum import not found");
        }
        return CurriculumImport.of(responses.getFirst());
    }
}
