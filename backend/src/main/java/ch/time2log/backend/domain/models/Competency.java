package ch.time2log.backend.domain.models;

import ch.time2log.backend.infrastructure.supabase.responses.CompetencyResponse;

import java.util.List;
import java.util.UUID;

public record Competency(UUID id, String code, String description) {

    public static Competency of(CompetencyResponse r) {
        return new Competency(r.id(), r.code(), r.description());
    }

    public static List<Competency> ofList(List<CompetencyResponse> list) {
        return list.stream().map(Competency::of).toList();
    }
}
