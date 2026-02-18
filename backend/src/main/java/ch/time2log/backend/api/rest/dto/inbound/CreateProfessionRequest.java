package ch.time2log.backend.api.rest.dto.inbound;

public record CreateProfessionRequest(
        String key,
        String label
) {}
