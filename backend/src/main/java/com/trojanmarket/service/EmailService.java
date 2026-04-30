package com.trojanmarket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sends transactional emails (currently just verification codes).
 *
 * Transport is chosen at runtime in this priority order:
 *   1. SendGrid HTTP API — used when {@code SENDGRID_API_KEY} is set. Required on
 *      hosts (e.g. Railway) that block outbound SMTP, since this goes over HTTPS.
 *   2. SMTP via {@link JavaMailSender} — auto-configured when {@code spring.mail.host}
 *      is set. Useful for local dev or hosts that allow outbound SMTP.
 *   3. Logging fallback — code is logged at WARN so signup still works end-to-end
 *      while email is being wired up.
 *
 * Failures never crash the caller; the code is always written to logs at ERROR
 * level so it can be recovered manually if delivery fails.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final URI SENDGRID_URI = URI.create("https://api.sendgrid.com/v3/mail/send");
    private static final Pattern FROM_PATTERN = Pattern.compile("^\\s*(.+?)\\s*<\\s*(.+?)\\s*>\\s*$");

    private final String sendgridApiKey;
    private final String fromAddress;
    private final String fromName;
    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public EmailService(@Value("${app.sendgrid.api-key:}") String sendgridApiKey,
                        @Value("${app.mail.from:Trojan Market <trojanmarket.noreply@gmail.com>}") String from,
                        @Autowired(required = false) JavaMailSender mailSender,
                        ObjectMapper objectMapper) {
        this.sendgridApiKey = sendgridApiKey == null ? "" : sendgridApiKey.trim();
        this.mailSender = mailSender;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // Accept either "Name <email@host>" or just "email@host" for app.mail.from.
        Matcher m = FROM_PATTERN.matcher(from == null ? "" : from);
        if (m.matches()) {
            this.fromName = m.group(1);
            this.fromAddress = m.group(2);
        } else {
            this.fromName = null;
            this.fromAddress = from == null ? "" : from.trim();
        }
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

        if (!sendgridApiKey.isBlank()) {
            sendViaSendGrid(to, subject, body, code);
            return;
        }
        if (mailSender != null) {
            sendViaSmtp(to, subject, body, code);
            return;
        }
        log.warn("[EmailService] No email transport configured. Verification code for {}: {}", to, code);
    }

    // --- transports ---------------------------------------------------------

    private void sendViaSendGrid(String to, String subject, String body, String code) {
        try {
            Map<String, Object> fromObj = new LinkedHashMap<>();
            fromObj.put("email", fromAddress);
            if (fromName != null) {
                fromObj.put("name", fromName);
            }
            Map<String, Object> payload = Map.of(
                    "personalizations", List.of(Map.of("to", List.of(Map.of("email", to)))),
                    "from", fromObj,
                    "subject", subject,
                    "content", List.of(Map.of("type", "text/plain", "value", body))
            );
            String json = objectMapper.writeValueAsString(payload);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(SENDGRID_URI)
                    .header("Authorization", "Bearer " + sendgridApiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 == 2) {
                log.info("Sent verification code to {} via SendGrid (status {})", to, resp.statusCode());
            } else {
                log.error("[EmailService] SendGrid returned {} for {} — code is: {} | response: {}",
                        resp.statusCode(), to, code, resp.body());
            }
        } catch (Exception e) {
            log.error("[EmailService] SendGrid send failed for {} — code is: {} (cause: {})",
                    to, code, e.getMessage());
        }
    }

    private void sendViaSmtp(String to, String subject, String body, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromName != null ? fromName + " <" + fromAddress + ">" : fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
            log.info("Sent verification code to {} via SMTP", to);
        } catch (Exception e) {
            log.error("[EmailService] SMTP send failed for {} — code is: {} (cause: {})",
                    to, code, e.getMessage());
        }
    }
}
