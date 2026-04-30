package com.trojanmarket.service;

import com.trojanmarket.dto.AuthResponseDTO;
import com.trojanmarket.dto.LoginRequest;
import com.trojanmarket.dto.SignupRequest;
import com.trojanmarket.dto.VerifyEmailRequest;
import com.trojanmarket.entity.User;
import com.trojanmarket.repository.UserRepository;
import com.trojanmarket.security.ForbiddenException;
import com.trojanmarket.security.JwtService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class AuthService {

    private static final String USC_DOMAIN = "@usc.edu";
    private static final int BAN_THRESHOLD = 10;
    private static final Duration CODE_TTL = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public void signup(SignupRequest req) {
        String email = normalizeEmail(req.getEmail());
        validateUSCEmail(email);

        User existing = userRepository.findByEmail(email).orElse(null);
        if (existing != null && Boolean.TRUE.equals(existing.getIsVerified())) {
            throw new IllegalArgumentException("An account with that email already exists");
        }

        String code = generateCode();
        LocalDateTime expiresAt = LocalDateTime.now().plus(CODE_TTL);

        User user;
        if (existing != null) {
            // Re-signing up an unverified account: refresh password, name, and code.
            existing.setFirstName(req.getFirstName().trim());
            existing.setLastName(req.getLastName().trim());
            existing.setPassword(passwordEncoder.encode(req.getPassword()));
            existing.setVerificationCode(code);
            existing.setVerificationCodeExpiresAt(expiresAt);
            user = userRepository.save(existing);
        } else {
            user = userRepository.save(User.builder()
                    .username(deriveUsername(email))
                    .password(passwordEncoder.encode(req.getPassword()))
                    .firstName(req.getFirstName().trim())
                    .lastName(req.getLastName().trim())
                    .email(email)
                    .isVerified(false)
                    .verificationCode(code)
                    .verificationCodeExpiresAt(expiresAt)
                    .review(0)
                    .reviewCount(0)
                    .isActive(true)
                    .build());
        }

        emailService.sendVerificationCode(user.getEmail(), user.getFirstName(), code);
    }

    @Transactional
    public AuthResponseDTO verifyEmail(VerifyEmailRequest req) {
        String email = normalizeEmail(req.getEmail());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid code or email"));

        if (Boolean.TRUE.equals(user.getIsVerified())) {
            throw new IllegalArgumentException("Email is already verified — please log in");
        }
        if (user.getVerificationCode() == null
                || user.getVerificationCodeExpiresAt() == null
                || user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Verification code has expired — please request a new one");
        }
        if (!user.getVerificationCode().equals(req.getCode().trim())) {
            throw new IllegalArgumentException("Invalid verification code");
        }

        user.setIsVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);

        return issueAuthResponse(user);
    }

    @Transactional
    public void resendVerification(String emailRaw) {
        String email = normalizeEmail(emailRaw);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("No account found for that email"));
        if (Boolean.TRUE.equals(user.getIsVerified())) {
            throw new IllegalArgumentException("Email is already verified — please log in");
        }

        String code = generateCode();
        user.setVerificationCode(code);
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plus(CODE_TTL));
        userRepository.save(user);

        emailService.sendVerificationCode(user.getEmail(), user.getFirstName(), code);
    }

    public AuthResponseDTO login(LoginRequest req) {
        String email = normalizeEmail(req.getEmail());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ForbiddenException("Invalid email or password"));

        if (!Boolean.TRUE.equals(user.getIsVerified())) {
            throw new ForbiddenException("Please verify your email before logging in");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new ForbiddenException("Invalid email or password");
        }

        return issueAuthResponse(user);
    }

    public void validateUSCEmail(String email) {
        if (email == null || !email.toLowerCase().endsWith(USC_DOMAIN)) {
            throw new ForbiddenException("Only @usc.edu emails are accepted");
        }
    }

    public boolean isBannedUser(Integer userID) {
        // TODO: when a Reports table is added, count reports where reportedUserID = userID
        //       and return count >= BAN_THRESHOLD. CLAUDE.md schema does not yet define
        //       this table, so treat all users as not-banned for now.
        return false;
    }

    // --- internals ----------------------------------------------------------

    private AuthResponseDTO issueAuthResponse(User user) {
        String token = jwtService.issueToken(user.getUserID(), user.getEmail(), user.getUsername());
        return AuthResponseDTO.builder()
                .token(token)
                .userID(user.getUserID())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .isVerified(user.getIsVerified())
                .build();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String generateCode() {
        // 6-digit zero-padded numeric code, e.g. "048213".
        return String.format("%06d", random.nextInt(1_000_000));
    }

    private String deriveUsername(String email) {
        String local = email.substring(0, email.indexOf('@'));
        String base = local.replaceAll("[^A-Za-z0-9_]", "");
        if (base.length() > 50) {
            base = base.substring(0, 50);
        }
        if (userRepository.existsByUsername(base)) {
            return base + "_" + System.currentTimeMillis();
        }
        return base;
    }
}
