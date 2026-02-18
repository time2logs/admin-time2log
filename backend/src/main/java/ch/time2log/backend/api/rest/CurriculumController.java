package ch.time2log.backend.api.rest;

import ch.time2log.backend.api.rest.dto.outbound.CompetencyDto;
import ch.time2log.backend.api.rest.dto.outbound.CurriculumNodeDto;
import ch.time2log.backend.api.rest.dto.outbound.CurriculumOverviewDto;
import ch.time2log.backend.domain.CurriculumDomainService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequestMapping("api/organizations/{organizationId}/professions/{professionId}/curriculum")
@RestController
public class CurriculumController {

    private final CurriculumDomainService curriculumDomainService;

    public CurriculumController(CurriculumDomainService curriculumDomainService) {
        this.curriculumDomainService = curriculumDomainService;
    }

    @GetMapping
    public CurriculumOverviewDto getCurriculum(
            @PathVariable UUID organizationId,
            @PathVariable UUID professionId) {
        var nodes = curriculumDomainService.getNodes(organizationId, professionId)
                .stream().map(CurriculumNodeDto::of).toList();
        var competencies = curriculumDomainService.getCompetencies(organizationId, professionId)
                .stream().map(CompetencyDto::of).toList();
        return new CurriculumOverviewDto(nodes, competencies);
    }
}
