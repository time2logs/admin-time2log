package ch.time2log.backend.api.rest.dto.outbound;

import ch.time2log.backend.infrastructure.supabase.responses.OrganizationResponse;

import java.util.List;
import java.util.UUID;

public record OrganizationDto(
   UUID id,
   String name
) {
    public static OrganizationDto of(OrganizationResponse organizationResponse) {
        return new OrganizationDto(organizationResponse.id(), organizationResponse.name());
    }

    public static List<OrganizationDto> ofList(List<OrganizationResponse> organizationResponseList) {
        return organizationResponseList.stream().map(OrganizationDto::of).toList();
    }
}
