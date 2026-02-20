package ch.time2log.backend.infrastructure.mail;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class InviteMailService {
    private static final Logger log = LoggerFactory.getLogger(InviteMailService.class);

    private final JavaMailSender mailSender;
    private final ResourceLoader resourceLoader;

    @Value("${app.mail.from:noreply@time2log.app}")
    private String from;

    public InviteMailService(JavaMailSender mailSender, ResourceLoader resourceLoader) {
        this.mailSender = mailSender;
        this.resourceLoader = resourceLoader;
    }

    public void sendInvite(String toEmail, String organizationName, String role, String actionLink) {
        try {
            var html = loadTemplate(organizationName, role, actionLink);

            MimeMessage message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject("You've been invited to " + organizationName);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Invite email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send invite email to {}: {}", toEmail, e.getMessage());
            log.info("Invite link for {} (visit manually): {}", toEmail, actionLink);
        }
    }

    private String loadTemplate(String organizationName, String role, String actionLink) throws IOException {
        var resource = resourceLoader.getResource("classpath:templates/mail/invite.html");
        var template = resource.getContentAsString(StandardCharsets.UTF_8);

        var isAdmin = "admin".equalsIgnoreCase(role);
        return template
                .replace("{{SUBJECT}}", "You've been invited!")
                .replace("{{ORGANIZATION_NAME}}", organizationName)
                .replace("{{ROLE_CLASS}}", isAdmin ? "role-badge role-admin" : "role-badge role-member")
                .replace("{{ROLE_LABEL}}", isAdmin ? "Admin" : "Member")
                .replace("{{ACTION_LINK}}", actionLink);
    }
}
