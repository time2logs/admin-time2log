package ch.time2log.backend.api.rest.dto.outbound;

import ch.time2log.backend.domain.models.Organization;

import java.util.List;
import java.util.UUID;

public record OrganizationDto(
   UUID id,
   String name
) {
    public static OrganizationDto of(Organization org) {
        return new OrganizationDto(org.id(), org.name());
    }

    public static List<OrganizationDto> ofList(List<Organization> list) {
        return list.stream().map(OrganizationDto::of).toList();
    }
}
