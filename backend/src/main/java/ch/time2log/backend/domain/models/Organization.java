package ch.time2log.backend.domain.models;

import ch.time2log.backend.infrastructure.supabase.responses.OrganizationResponse;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record Organization(UUID id, String name, OffsetDateTime createdAt, OffsetDateTime updatedAt) {

    public static Organization of(OrganizationResponse r) {
        return new Organization(r.id(), r.name(), r.created_at(), r.updated_at());
    }

    public static List<Organization> ofList(List<OrganizationResponse> list) {
        return list.stream().map(Organization::of).toList();
    }
}
