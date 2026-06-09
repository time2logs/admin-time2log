package ch.time2log.backend.infrastructure.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class InviteMailService {
    private static final Logger log = LoggerFactory.getLogger(InviteMailService.class);

    private final EmailSender emailSender;
    private final ResourceLoader resourceLoader;

    @Value("${app.mail.from:noreply@time2log.app}")
    private String from;

    public InviteMailService(EmailSender emailSender, ResourceLoader resourceLoader) {
        this.emailSender = emailSender;
        this.resourceLoader = resourceLoader;
    }

    public void sendInvite(String toEmail, String organizationName, String role, String actionLink) {
        try {
            var html = loadTemplate(organizationName, role, actionLink);
            emailSender.send(from, toEmail, "Einladung zu " + organizationName, html);
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
        var isModerator = "moderator".equalsIgnoreCase(role);

        String roleClass;
        String roleLabel;
        if (isAdmin) {
            roleClass = "role-badge role-admin";
            roleLabel = "Admin";
        } else if (isModerator) {
            roleClass = "role-badge role-moderator";
            roleLabel = "Moderator";
        } else {
            roleClass = "role-badge role-member";
            roleLabel = "Member";
        }

        return template
                .replace("{{SUBJECT}}", "Du wurdest eingeladen!")
                .replace("{{ORGANIZATION_NAME}}", organizationName)
                .replace("{{ROLE_CLASS}}", roleClass)
                .replace("{{ROLE_LABEL}}", roleLabel)
                .replace("{{ACTION_LINK}}", actionLink);
    }
}
