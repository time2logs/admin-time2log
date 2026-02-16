package ch.time2log.backend.api.rest.dto.inbound;

public record CreateActivityRequest(
        String key,
        String label,
        String description,
        String category
) {}
