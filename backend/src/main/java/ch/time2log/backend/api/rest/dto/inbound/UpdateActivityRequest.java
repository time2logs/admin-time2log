package ch.time2log.backend.api.rest.dto.inbound;

public record UpdateActivityRequest(
        String key,
        String label,
        String description,
        String category,
        boolean isActive
) {}
