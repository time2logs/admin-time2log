package ch.time2log.backend.api.rest.dto.outbound;

import ch.time2log.backend.domain.models.CurriculumImport;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CurriculumImportDto(
        UUID id,
        String version,
        String status,
        String error,
        OffsetDateTime created_at
) {
    public static CurriculumImportDto of(CurriculumImport i) {
        return new CurriculumImportDto(i.id(), i.version(), i.status(), i.error(), i.createdAt());
    }

    public static List<CurriculumImportDto> ofList(List<CurriculumImport> list) {
        return list.stream().map(CurriculumImportDto::of).toList();
    }
}
