package ch.time2log.backend.api.rest.dto.outbound;

import ch.time2log.backend.infrastructure.supabase.responses.CurriculumImportResponse;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CurriculumImportDto(
        UUID id,
        String version,
        String status,
        String error,
        OffsetDateTime created_at
) {
    public static CurriculumImportDto of(CurriculumImportResponse response) {
        return new CurriculumImportDto(
                response.id(),
                response.version(),
                response.status(),
                response.error(),
                response.created_at()
        );
    }
}
