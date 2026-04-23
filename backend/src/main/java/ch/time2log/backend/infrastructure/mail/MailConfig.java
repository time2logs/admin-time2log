package ch.time2log.backend.infrastructure.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class MailConfig {

    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    @Bean
    @ConditionalOnProperty(name = "app.mail.provider", havingValue = "resend")
    public EmailSender resendEmailSender(@Value("${app.mail.resend-api-key}") String apiKey) {
        log.info("Using Resend HTTP API as mail provider");
        return new ResendEmailSender(apiKey);
    }

    @Bean
    @ConditionalOnProperty(name = "app.mail.provider", havingValue = "smtp", matchIfMissing = true)
    public EmailSender smtpEmailSender(JavaMailSender mailSender) {
        log.info("Using SMTP as mail provider");
        return new SmtpEmailSender(mailSender);
    }
}
