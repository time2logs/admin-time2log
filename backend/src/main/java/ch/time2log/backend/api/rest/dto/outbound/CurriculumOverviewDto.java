package ch.time2log.backend.api.rest.dto.outbound;

import java.util.List;

public record CurriculumOverviewDto(
        List<CurriculumNodeDto> nodes,
        List<CompetencyDto> competencies
) {}
