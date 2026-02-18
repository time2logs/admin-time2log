package ch.time2log.backend.api.rest;

import ch.time2log.backend.api.rest.dto.inbound.CreateProfessionRequest;
import ch.time2log.backend.api.rest.dto.outbound.ProfessionDto;
import ch.time2log.backend.domain.ProfessionDomainService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("api/organizations/{organizationId}/professions")
@RestController
public class ProfessionController {

    private final ProfessionDomainService professionDomainService;

    public ProfessionController(ProfessionDomainService professionDomainService) {
        this.professionDomainService = professionDomainService;
    }

    @GetMapping
    public List<ProfessionDto> getProfessions(@PathVariable UUID organizationId) {
        return ProfessionDto.ofList(professionDomainService.getProfessions(organizationId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfessionDto createProfession(
            @PathVariable UUID organizationId,
            @RequestBody CreateProfessionRequest request) {
        return ProfessionDto.of(professionDomainService.createProfession(organizationId, request.key(), request.label()));
    }
}
