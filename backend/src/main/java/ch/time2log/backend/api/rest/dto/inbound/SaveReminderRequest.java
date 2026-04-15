package ch.time2log.backend.api.rest.dto.inbound;

public record SaveReminderRequest(
        String channel,
        String sendTime,
        int idleDays,
        String sendDay
) {}
