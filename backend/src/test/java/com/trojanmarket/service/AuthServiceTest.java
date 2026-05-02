package com.trojanmarket.service;

import com.trojanmarket.dto.AuthResponseDTO;
import com.trojanmarket.dto.LoginRequest;
import com.trojanmarket.dto.SignupRequest;
import com.trojanmarket.dto.VerifyEmailRequest;
import com.trojanmarket.entity.User;
import com.trojanmarket.repository.UserRepository;
import com.trojanmarket.security.ForbiddenException;
import com.trojanmarket.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthService}.
 *
 * Maps to the test plan's "Authentication System" unit + black-box + white-box +
 * regression sections, adapted to the email/password flow that replaced USC SSO:
 *   - signup (replaces handleSSOLogin / createUserIfNotExists for new accounts)
 *   - verifyEmail (verifies the 6-digit code)
 *   - login (BCrypt + isVerified guard)
 *   - validateUSCEmail (white-box — only @usc.edu passes)
 *   - isBannedUser (white-box — report-count threshold; currently a stub returning false)
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock JwtService jwtService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock EmailService emailService;
    @InjectMocks AuthService authService;

    @Nested
    @DisplayName("validateUSCEmail")
    class ValidateUSCEmail {

        @Test
        void uscEmailPasses() {
            // No exception thrown.
            authService.validateUSCEmail("ttrojan@usc.edu");
            authService.validateUSCEmail("First.Last@usc.edu");
        }

        @Test
        void nonUscEmailRejected() {
            assertThatThrownBy(() -> authService.validateUSCEmail("user@gmail.com"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("@usc.edu");
        }

        @Test
        void nullEmailRejected() {
            assertThatThrownBy(() -> authService.validateUSCEmail(null))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        void caseInsensitiveDomainAccepted() {
            authService.validateUSCEmail("ttrojan@USC.EDU");
        }
    }

    @Nested
    @DisplayName("isBannedUser (report threshold stub)")
    class IsBannedUser {

        @Test
        void returnsFalseWhileNoReportsTableExists() {
            // CLAUDE.md: schema does not yet define a Reports table — current stub
            // always returns false. This test pins that contract so adding the
            // table later forces a deliberate update.
            assertThat(authService.isBannedUser(1)).isFalse();
            assertThat(authService.isBannedUser(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("signup")
    class Signup {

        private SignupRequest req;

        @BeforeEach
        void setUp() {
            req = new SignupRequest("Tommy", "Trojan", "ttrojan@usc.edu", "ChangeMe1!");
        }

        @Test
        void signupRejectsNonUscEmail() {
            req.setEmail("hacker@gmail.com");
            assertThatThrownBy(() -> authService.signup(req))
                    .isInstanceOf(ForbiddenException.class);
            verify(userRepository, never()).save(any());
            verify(emailService, never()).sendVerificationCode(anyString(), anyString(), anyString());
        }

        @Test
        void signupRejectsExistingVerifiedAccount() {
            User existing = User.builder().userID(1).email("ttrojan@usc.edu").isVerified(true).build();
            when(userRepository.findByEmail("ttrojan@usc.edu")).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> authService.signup(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");
            verify(emailService, never()).sendVerificationCode(anyString(), anyString(), anyString());
        }

        @Test
        void signupCreatesUserWhenEmailIsNew() {
            when(userRepository.findByEmail("ttrojan@usc.edu")).thenReturn(Optional.empty());
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(passwordEncoder.encode("ChangeMe1!")).thenReturn("HASHED");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setUserID(42);
                return u;
            });

            authService.signup(req);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User saved = captor.getValue();
            assertThat(saved.getEmail()).isEqualTo("ttrojan@usc.edu");
            assertThat(saved.getFirstName()).isEqualTo("Tommy");
            assertThat(saved.getLastName()).isEqualTo("Trojan");
            assertThat(saved.getPassword()).isEqualTo("HASHED");
            assertThat(saved.getIsVerified()).isFalse();
            assertThat(saved.getVerificationCode()).hasSize(6).matches("\\d{6}");
            assertThat(saved.getVerificationCodeExpiresAt()).isAfter(LocalDateTime.now());

            // Email goes out with the same code that was persisted.
            verify(emailService).sendVerificationCode("ttrojan@usc.edu", "Tommy", saved.getVerificationCode());
        }

        @Test
        void signupResetsCodeOnAlreadyExistingUnverifiedAccount() {
            User existing = User.builder()
                    .userID(7).email("ttrojan@usc.edu").isVerified(false)
                    .verificationCode("000000").build();
            when(userRepository.findByEmail("ttrojan@usc.edu")).thenReturn(Optional.of(existing));
            when(passwordEncoder.encode("ChangeMe1!")).thenReturn("HASHED");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            authService.signup(req);

            assertThat(existing.getVerificationCode()).matches("\\d{6}").isNotEqualTo("000000");
            assertThat(existing.getPassword()).isEqualTo("HASHED");
            verify(emailService).sendVerificationCode(
                    org.mockito.ArgumentMatchers.eq("ttrojan@usc.edu"),
                    org.mockito.ArgumentMatchers.eq("Tommy"),
                    org.mockito.ArgumentMatchers.anyString());
        }
    }

    @Nested
    @DisplayName("verifyEmail")
    class VerifyEmail {

        @Test
        void verifyMarksUserVerifiedAndIssuesJwt() {
            User user = User.builder()
                    .userID(1).email("ttrojan@usc.edu").username("ttrojan")
                    .firstName("Tommy").lastName("Trojan")
                    .verificationCode("123456")
                    .verificationCodeExpiresAt(LocalDateTime.now().plusMinutes(10))
                    .isVerified(false).build();
            when(userRepository.findByEmail("ttrojan@usc.edu")).thenReturn(Optional.of(user));
            when(jwtService.issueToken(1, "ttrojan@usc.edu", "ttrojan")).thenReturn("JWT");

            AuthResponseDTO resp = authService.verifyEmail(
                    new VerifyEmailRequest("ttrojan@usc.edu", "123456"));

            assertThat(user.getIsVerified()).isTrue();
            assertThat(user.getVerificationCode()).isNull();
            assertThat(user.getVerificationCodeExpiresAt()).isNull();
            assertThat(resp.getToken()).isEqualTo("JWT");
            assertThat(resp.getEmail()).isEqualTo("ttrojan@usc.edu");
            assertThat(resp.getFirstName()).isEqualTo("Tommy");
            verify(userRepository).save(user);
        }

        @Test
        void verifyRejectsWrongCode() {
            User user = User.builder()
                    .email("ttrojan@usc.edu").verificationCode("123456")
                    .verificationCodeExpiresAt(LocalDateTime.now().plusMinutes(10))
                    .isVerified(false).build();
            when(userRepository.findByEmail("ttrojan@usc.edu")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.verifyEmail(
                    new VerifyEmailRequest("ttrojan@usc.edu", "000000")))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(user.getIsVerified()).isFalse();
            verify(jwtService, never()).issueToken(any(), any(), any());
        }

        @Test
        void verifyRejectsExpiredCode() {
            User user = User.builder()
                    .email("ttrojan@usc.edu").verificationCode("123456")
                    .verificationCodeExpiresAt(LocalDateTime.now().minusMinutes(1))
                    .isVerified(false).build();
            when(userRepository.findByEmail("ttrojan@usc.edu")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.verifyEmail(
                    new VerifyEmailRequest("ttrojan@usc.edu", "123456")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        void verifyRejectsAlreadyVerifiedUser() {
            User user = User.builder()
                    .email("ttrojan@usc.edu").isVerified(true).build();
            when(userRepository.findByEmail("ttrojan@usc.edu")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.verifyEmail(
                    new VerifyEmailRequest("ttrojan@usc.edu", "123456")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already verified");
        }

        @Test
        void verifyRejectsUnknownEmail() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            assertThatThrownBy(() -> authService.verifyEmail(
                    new VerifyEmailRequest("nobody@usc.edu", "123456")))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        void loginReturnsJwtForValidCredentials() {
            User user = User.builder()
                    .userID(1).email("ttrojan@usc.edu").username("ttrojan")
                    .firstName("Tommy").lastName("Trojan")
                    .password("HASHED").isVerified(true).build();
            when(userRepository.findByEmail("ttrojan@usc.edu")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("ChangeMe1!", "HASHED")).thenReturn(true);
            when(jwtService.issueToken(1, "ttrojan@usc.edu", "ttrojan")).thenReturn("JWT");

            AuthResponseDTO resp = authService.login(new LoginRequest("ttrojan@usc.edu", "ChangeMe1!"));

            assertThat(resp.getToken()).isEqualTo("JWT");
            assertThat(resp.getFirstName()).isEqualTo("Tommy");
        }

        @Test
        void loginRejectsUnverifiedAccount() {
            User user = User.builder().email("ttrojan@usc.edu").password("HASHED").isVerified(false).build();
            when(userRepository.findByEmail("ttrojan@usc.edu")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.login(new LoginRequest("ttrojan@usc.edu", "ChangeMe1!")))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("verify");
            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        void loginRejectsWrongPassword() {
            User user = User.builder().email("ttrojan@usc.edu").password("HASHED").isVerified(true).build();
            when(userRepository.findByEmail("ttrojan@usc.edu")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("WrongPassword", "HASHED")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(new LoginRequest("ttrojan@usc.edu", "WrongPassword")))
                    .isInstanceOf(ForbiddenException.class);
            verify(jwtService, never()).issueToken(any(), any(), any());
        }

        @Test
        void loginRejectsUnknownEmail() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@usc.edu", "x")))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        void emailIsCaseInsensitiveAndTrimmed() {
            User user = User.builder()
                    .userID(1).email("ttrojan@usc.edu").username("ttrojan")
                    .password("HASHED").isVerified(true).build();
            when(userRepository.findByEmail("ttrojan@usc.edu")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("p", "HASHED")).thenReturn(true);
            when(jwtService.issueToken(any(), any(), any())).thenReturn("JWT");

            authService.login(new LoginRequest("  TTROJAN@USC.EDU  ", "p"));
            verify(userRepository, times(1)).findByEmail("ttrojan@usc.edu");
        }
    }
}
