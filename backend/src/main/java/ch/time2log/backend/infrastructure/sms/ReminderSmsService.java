package ch.time2log.backend.infrastructure.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class ReminderSmsService {
    private static final Logger log = LoggerFactory.getLogger(ReminderSmsService.class);
    private static final String SWISSCOM_SMS_URL = "https://api.swisscom.com/messaging/sms";

    private final HttpClient httpClient;

    @Value("${app.sms.swisscom.client-id:}")
    private String clientId;

    @Value("${app.sms.swisscom.from:}")
    private String sender;

    public ReminderSmsService() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public void sendReminder(String phoneNumber, String firstName, String organizationName, long daysInactive) {
        if (clientId == null || clientId.isBlank()) {
            log.warn("Swisscom SMS client ID not configured, skipping SMS to {}", phoneNumber);
            return;
        }

        var messageText = String.format(
                "Hallo %s, du hast seit %d Tagen keine Aktivitaeten in %s erfasst. Bitte logge deine Stunden auf time2log.",
                firstName, daysInactive, organizationName
        );

        try {
            var fromField = (sender != null && !sender.isBlank())
                    ? String.format("\"from\":\"%s\",", sender)
                    : "";

            var jsonBody = String.format(
                    "{%s\"to\":\"%s\",\"text\":\"%s\"}",
                    fromField, phoneNumber, escapeJson(messageText)
            );

            var request = HttpRequest.newBuilder()
                    .uri(URI.create(SWISSCOM_SMS_URL))
                    .header("client_id", clientId)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("SCS-Version", "2")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                var messageId = response.headers().firstValue("SCS-MessageId").orElse("unknown");
                log.info("SMS reminder sent to {} (messageId={})", phoneNumber, messageId);
            } else {
                log.error("Swisscom SMS failed ({}): {}", response.statusCode(), response.body());
            }
        } catch (IOException | InterruptedException e) {
            log.error("Failed to send SMS reminder to {}: {}", phoneNumber, e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
