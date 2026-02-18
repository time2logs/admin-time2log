package ch.time2log.backend.domain.models;

import ch.time2log.backend.infrastructure.supabase.responses.CurriculumImportResponse;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CurriculumImport(UUID id, String version, String status, String error, OffsetDateTime createdAt) {

    public static CurriculumImport of(CurriculumImportResponse r) {
        return new CurriculumImport(r.id(), r.version(), r.status(), r.error(), r.created_at());
    }

    public static List<CurriculumImport> ofList(List<CurriculumImportResponse> list) {
        return list.stream().map(CurriculumImport::of).toList();
    }
}
