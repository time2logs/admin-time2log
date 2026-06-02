package ch.time2log.backend.infrastructure.mail;

/**
 * Abstraction for sending HTML emails.
 * Implementations can use SMTP (JavaMailSender) or HTTP APIs (Resend).
 */
public interface EmailSender {

    void send(String from, String to, String subject, String htmlBody);
}
