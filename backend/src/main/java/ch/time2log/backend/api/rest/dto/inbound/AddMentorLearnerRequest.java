package ch.time2log.backend.api.rest.dto.inbound;

import java.util.UUID;

public record AddMentorLearnerRequest(
        UUID learnerId
) {}
