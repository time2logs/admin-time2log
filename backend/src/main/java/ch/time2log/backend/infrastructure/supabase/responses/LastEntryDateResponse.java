package ch.time2log.backend.infrastructure.supabase.responses;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LastEntryDateResponse(
        UUID user_id,
        OffsetDateTime last_entry_date
) {}
