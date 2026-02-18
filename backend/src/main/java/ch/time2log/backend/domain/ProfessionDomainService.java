package ch.time2log.backend.domain;

import ch.time2log.backend.domain.exception.EntityNotCreatedException;
import ch.time2log.backend.domain.models.Profession;
import ch.time2log.backend.infrastructure.supabase.SupabaseService;
import ch.time2log.backend.infrastructure.supabase.responses.ProfessionResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProfessionDomainService {

    private final SupabaseService supabaseService;

    public ProfessionDomainService(SupabaseService supabaseService) {
        this.supabaseService = supabaseService;
    }

    public List<Profession> getProfessions(UUID organizationId) {
        var responses = supabaseService.getListWithQuery(
                "admin.professions",
                "organization_id=eq." + organizationId,
                ProfessionResponse.class
        );
        return Profession.ofList(responses);
    }

    public Profession createProfession(UUID organizationId, String key, String label) {
        var body = Map.of(
                "organization_id", organizationId,
                "key", key,
                "label", label
        );
        var created = supabaseService.post("admin.professions", body, ProfessionResponse[].class);
        if (created == null || created.length == 0) {
            throw new EntityNotCreatedException("Profession could not be created");
        }
        return Profession.of(created[0]);
    }
}
