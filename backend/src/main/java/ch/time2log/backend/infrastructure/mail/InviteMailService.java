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
        sendInviteInternal(toEmail, organizationName, role, actionLink, false);
    }

    /**
     * Variant for users that already have an account on the platform. The mail
     * does not ask them to "set up" anything — the click leads to login, then
     * directly joins them to the new organization.
     */
    public void sendOrgJoinInvite(String toEmail, String organizationName, String role, String actionLink) {
        sendInviteInternal(toEmail, organizationName, role, actionLink, true);
    }

    private void sendInviteInternal(String toEmail, String organizationName, String role, String actionLink, boolean existingUser) {
        try {
            var html = loadTemplate(organizationName, role, actionLink, existingUser);
            emailSender.send(from, toEmail, "Einladung zu " + organizationName, html);
            log.info("Invite email sent to {} (existingUser={})", toEmail, existingUser);
        } catch (Exception e) {
            log.error("Failed to send invite email to {}: {}", toEmail, e.getMessage());
            log.info("Invite link for {} (visit manually): {}", toEmail, actionLink);
        }
    }

    private String loadTemplate(String organizationName, String role, String actionLink, boolean existingUser) throws IOException {
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

        var headerSubtitle = existingUser
                ? "Melde dich an und tritt der Organisation bei."
                : "Richte dein Konto ein und tritt der Organisation bei.";
        var bodyMessage = existingUser
                ? "Du wurdest eingeladen, <strong>" + organizationName + "</strong> auf Time2Log beizutreten. "
                  + "Klicke auf den Button unten, melde dich mit deinem bestehenden Konto an, und du wirst automatisch hinzugefügt. "
                  + "Dieser Link ist <strong>24 Stunden</strong> gültig."
                : "Du wurdest eingeladen, <strong>" + organizationName + "</strong> auf Time2Log beizutreten. "
                  + "Klicke auf den Button unten, um die Einladung anzunehmen und dein Konto einzurichten. "
                  + "Dieser Link ist <strong>24 Stunden</strong> gültig.";
        var buttonLabel = existingUser ? "Anmelden &amp; beitreten" : "Einladung annehmen";

        return template
                .replace("{{SUBJECT}}", "Du wurdest eingeladen!")
                .replace("{{HEADER_SUBTITLE}}", headerSubtitle)
                .replace("{{BODY_MESSAGE}}", bodyMessage)
                .replace("{{BUTTON_LABEL}}", buttonLabel)
                .replace("{{ORGANIZATION_NAME}}", organizationName)
                .replace("{{ROLE_CLASS}}", roleClass)
                .replace("{{ROLE_LABEL}}", roleLabel)
                .replace("{{ACTION_LINK}}", actionLink);
    }
}
