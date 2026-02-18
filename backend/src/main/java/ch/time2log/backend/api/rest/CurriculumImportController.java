package ch.time2log.backend.api.rest;

import ch.time2log.backend.api.rest.dto.inbound.CreateCurriculumImportRequest;
import ch.time2log.backend.api.rest.dto.outbound.CurriculumImportDto;
import ch.time2log.backend.domain.CurriculumImportDomainService;
import ch.time2log.backend.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("api/organizations/{organizationId}/professions/{professionId}/curriculum-imports")
@RestController
public class CurriculumImportController {

    private final CurriculumImportDomainService curriculumImportDomainService;

    public CurriculumImportController(CurriculumImportDomainService curriculumImportDomainService) {
        this.curriculumImportDomainService = curriculumImportDomainService;
    }

    @GetMapping
    public List<CurriculumImportDto> getImports(
            @PathVariable UUID organizationId,
            @PathVariable UUID professionId) {
        return CurriculumImportDto.ofList(curriculumImportDomainService.getImports(organizationId, professionId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CurriculumImportDto createImport(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID organizationId,
            @PathVariable UUID professionId,
            @RequestBody CreateCurriculumImportRequest request) {
        return CurriculumImportDto.of(curriculumImportDomainService.createImport(organizationId, professionId, user.id(), request, request.version()));
    }

    @PostMapping("/{importId}/apply")
    public CurriculumImportDto applyImport(
            @PathVariable UUID organizationId,
            @PathVariable UUID professionId,
            @PathVariable UUID importId) {
        return CurriculumImportDto.of(curriculumImportDomainService.applyImport(importId));
    }
}
