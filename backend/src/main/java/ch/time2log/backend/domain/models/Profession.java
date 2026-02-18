package ch.time2log.backend.domain.models;

import ch.time2log.backend.infrastructure.supabase.responses.ProfessionResponse;

import java.util.List;
import java.util.UUID;

public record Profession(UUID id, UUID organizationId, String key, String label) {

    public static Profession of(ProfessionResponse r) {
        return new Profession(r.id(), r.organization_id(), r.key(), r.label());
    }

    public static List<Profession> ofList(List<ProfessionResponse> list) {
        return list.stream().map(Profession::of).toList();
    }
}
