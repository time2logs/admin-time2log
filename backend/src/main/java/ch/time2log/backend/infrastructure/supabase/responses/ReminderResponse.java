package ch.time2log.backend.infrastructure.supabase.responses;

import java.util.UUID;

public record ReminderResponse(
        UUID id,
        UUID organization_id,
        String channel,
        String send_time,
        int idle_days,
        String send_day
) {}
