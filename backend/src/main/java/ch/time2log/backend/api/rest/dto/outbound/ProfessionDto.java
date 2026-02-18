package ch.time2log.backend.api.rest.dto.outbound;

import ch.time2log.backend.domain.models.Profession;

import java.util.List;
import java.util.UUID;

public record ProfessionDto(
        UUID id,
        String key,
        String label
) {
    public static ProfessionDto of(Profession p) {
        return new ProfessionDto(p.id(), p.key(), p.label());
    }

    public static List<ProfessionDto> ofList(List<Profession> list) {
        return list.stream().map(ProfessionDto::of).toList();
    }
}
