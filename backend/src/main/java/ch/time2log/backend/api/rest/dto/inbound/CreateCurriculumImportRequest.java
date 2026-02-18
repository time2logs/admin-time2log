package ch.time2log.backend.api.rest.dto.inbound;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateCurriculumImportRequest(
        int schema_version,
        String version,
        String effective_from,
        ProfessionInfo profession,
        MetadataInfo metadata,
        List<CompetencyInfo> competencies,
        List<NodeInfo> nodes
) {
    public record ProfessionInfo(String key, String label) {}

    public record MetadataInfo(String source, String description, String language) {}

    public record CompetencyInfo(String code, String description) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record NodeInfo(
            String key,
            String label,
            String type,
            int order,
            List<String> competencies,
            List<NodeInfo> children
    ) {}
}
