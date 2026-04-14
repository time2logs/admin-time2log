package ch.time2log.backend.api.rest.dto.outbound;

import ch.time2log.backend.infrastructure.supabase.responses.ReminderResponse;

public record ReminderDto(
        String id,
        String channel,
        String sendTime,
        int idleDays,
        String sendDay
) {
    public static ReminderDto of(ReminderResponse r) {
        return new ReminderDto(r.id().toString(), r.channel(), r.send_time(), r.idle_days(), r.send_day());
    }
}
