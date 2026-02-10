package ch.time2log.backend.api.rest.dto;

import ch.time2log.backend.persistence.organization.OrganizationEntity;

import java.util.List;
import java.util.UUID;

public record OrganizationDto(
   UUID id,
   String name
) {
    public static OrganizationDto of(OrganizationEntity entity) {
        return new OrganizationDto(entity.getId(), entity.getName());
    }

    public static List<OrganizationDto> ofList(List<OrganizationEntity> entities) {
        return entities.stream().map(OrganizationDto::of).toList();
    }
}
