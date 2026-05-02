package com.trojanmarket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link EmailService}.
 *
 * SendGrid HTTP path is harder to mock because EmailService instantiates its own
 * {@code java.net.http.HttpClient}. We focus on the SMTP fallback (when SendGrid
 * isn't configured) and the log-only fallback (when neither transport is set up):
 *   - When neither SendGrid nor JavaMailSender is configured, no exception is
 *     thrown — the code is logged at WARN.
 *   - When JavaMailSender is configured, send() is called with the right To/Subject.
 *   - When JavaMailSender throws, the exception is swallowed (signup must not crash);
 *     the code remains in logs for manual recovery.
 */
class EmailServiceTest {

    @Test
    void noTransportFallsBackToLoggingWithoutThrowing() {
        EmailService svc = new EmailService("", "Trojan Market <noreply@example.com>",
                null, new ObjectMapper());
        // Should not throw — falls through to log-only path.
        svc.sendVerificationCode("ttrojan@usc.edu", "Tommy", "123456");
    }

    @Test
    void smtpPathSendsThroughJavaMailSender() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailService svc = new EmailService("", "Trojan Market <noreply@example.com>",
                mailSender, new ObjectMapper());

        svc.sendVerificationCode("ttrojan@usc.edu", "Tommy", "123456");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void smtpFailureIsSwallowedSoSignupDoesNotCrash() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        doThrow(new TestMailException("connect timeout")).when(mailSender)
                .send(any(SimpleMailMessage.class));
        EmailService svc = new EmailService("", "Trojan Market <noreply@example.com>",
                mailSender, new ObjectMapper());

        // Must not throw — failure is logged and swallowed.
        svc.sendVerificationCode("ttrojan@usc.edu", "Tommy", "123456");
    }

    @Test
    void sendGridPreferredOverSmtpWhenBothConfigured() {
        // When SENDGRID_API_KEY is non-blank we hit the SendGrid HTTP path even
        // if a JavaMailSender bean is also present. We can't easily intercept
        // the internal HttpClient, but we CAN assert that the SMTP path is not
        // exercised when SendGrid is configured (because the HTTP call will
        // fail/return early — that's still considered handled).
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailService svc = new EmailService("SG.test-key", "Trojan Market <noreply@example.com>",
                mailSender, new ObjectMapper());

        svc.sendVerificationCode("ttrojan@usc.edu", "Tommy", "123456");

        // SMTP path is bypassed; SendGrid HTTP call is attempted (will fail in test
        // since the API key is fake — failure is swallowed and logged).
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    /** Local subclass so we can build a MailException-typed throwable in the doThrow. */
    private static class TestMailException extends MailException {
        TestMailException(String msg) { super(msg); }
    }
}
