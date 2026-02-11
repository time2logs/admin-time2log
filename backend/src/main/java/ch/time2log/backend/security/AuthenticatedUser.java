package ch.time2log.backend.security;

import java.util.UUID;

public record AuthenticatedUser(
        UUID id,
        String email,
        String token
) {}
