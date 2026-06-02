package ch.time2log.backend.infrastructure.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

public class ResendEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailSender.class);

    private final WebClient webClient;

    public ResendEmailSender(String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public void send(String from, String to, String subject, String htmlBody) {
        var body = Map.of(
                "from", from,
                "to", List.of(to),
                "subject", subject,
                "html", htmlBody
        );

        var response = webClient.post()
                .uri("/emails")
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .block();

        if (response == null || !response.getStatusCode().is2xxSuccessful()) {
            var status = response == null ? "null" : response.getStatusCode().toString();
            throw new EmailSendException("Resend API returned: " + status);
        }

        log.debug("Resend email sent to {}", to);
    }
}
