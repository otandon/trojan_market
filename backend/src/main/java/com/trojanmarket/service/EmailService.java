package com.trojanmarket.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends transactional emails (currently just verification codes).
 *
 * If a {@link JavaMailSender} bean is present (auto-configured when
 * {@code spring.mail.host} is set), real SMTP is used. Otherwise the code is
 * logged at WARN level so signup still works end-to-end during local
 * development without SMTP credentials.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender,
                        @Value("${app.mail.from:Trojan Market <noreply@example.com>}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendVerificationCode(String to, String firstName, String code) {
        String subject = "Your Trojan Market verification code";
        String body = String.format(
                "Hi %s,%n%n" +
                        "Welcome to Trojan Market — the USC-exclusive marketplace for buying and selling%n" +
                        "with fellow Trojans.%n%n" +
                        "Your verification code is: %s%n%n" +
                        "Enter this code on the verification page to activate your account.%n" +
                        "This code expires in 15 minutes.%n%n" +
                        "If you did not create a Trojan Market account, you can ignore this email.%n%n" +
                        "— Trojan Market",
                firstName == null ? "there" : firstName, code);

        if (mailSender == null) {
            log.warn("[EmailService] SMTP not configured. Verification code for {}: {}", to, code);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
            log.info("Sent verification code to {}", to);
        } catch (Exception e) {
            // Don't crash signup if SMTP is unreachable (e.g. Railway blocks outbound
            // 587 to Gmail). Log loudly so the code can be recovered from logs and
            // returned to the user manually while a proper SMTP relay is wired up.
            // TODO: switch to a transactional API (SendGrid/Mailgun/Resend) for prod
            //       and re-throw here so failures are visible to the caller.
            log.error("[EmailService] FAILED to send to {} — code is: {} (cause: {})",
                    to, code, e.getMessage());
        }
    }
}
