package ch.time2log.backend.infrastructure.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class ReminderMailService {
    private static final Logger log = LoggerFactory.getLogger(ReminderMailService.class);

    private final EmailSender emailSender;
    private final ResourceLoader resourceLoader;

    @Value("${app.mail.from:noreply@time2log.app}")
    private String from;

    public ReminderMailService(EmailSender emailSender, ResourceLoader resourceLoader) {
        this.emailSender = emailSender;
        this.resourceLoader = resourceLoader;
    }

    public void sendReminder(String toEmail, String firstName, String organizationName, long daysInactive, String appLink) {
        try {
            var html = loadTemplate(firstName, organizationName, daysInactive, appLink);
            emailSender.send(from, toEmail, "Erinnerung: Erfasse deine Aktivitäten in " + organizationName, html);
            log.info("Reminder email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send reminder email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String loadTemplate(String firstName, String organizationName, long daysInactive, String appLink) throws IOException {
        var resource = resourceLoader.getResource("classpath:templates/mail/reminder.html");
        var template = resource.getContentAsString(StandardCharsets.UTF_8);

        return template
                .replace("{{FIRST_NAME}}", firstName)
                .replace("{{ORGANIZATION_NAME}}", organizationName)
                .replace("{{DAYS_INACTIVE}}", String.valueOf(daysInactive))
                .replace("{{APP_LINK}}", appLink);
    }
}
