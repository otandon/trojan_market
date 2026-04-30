package com.trojanmarket.controller;

import com.trojanmarket.dto.AuthResponseDTO;
import com.trojanmarket.dto.LoginRequest;
import com.trojanmarket.dto.ResendVerificationRequest;
import com.trojanmarket.dto.SignupRequest;
import com.trojanmarket.dto.VerifyEmailRequest;
import com.trojanmarket.entity.User;
import com.trojanmarket.repository.UserRepository;
import com.trojanmarket.security.AuthenticatedUser;
import com.trojanmarket.security.ForbiddenException;
import com.trojanmarket.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(@Valid @RequestBody SignupRequest req) {
        authService.signup(req);
        return ResponseEntity.ok(Map.of(
                "status", "verification_required",
                "email", req.getEmail().trim().toLowerCase(),
                "message", "We sent a 6-digit verification code to your USC email."));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponseDTO> verifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
        return ResponseEntity.ok(authService.verifyEmail(req));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(@Valid @RequestBody ResendVerificationRequest req) {
        authService.resendVerification(req.getEmail());
        return ResponseEntity.ok(Map.of("status", "sent"));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponseDTO> getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new ForbiddenException("Not authenticated");
        }
        // Pull fresh fields from the DB so /auth/me reflects the latest profile state.
        User user = userRepository.findById(principal.getUserID())
                .orElseThrow(() -> new ForbiddenException("Account no longer exists"));
        return ResponseEntity.ok(AuthResponseDTO.builder()
                .userID(user.getUserID())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .isVerified(user.getIsVerified())
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        // Stateless JWT — server-side logout clears the security context.
        // The client must also discard the token from localStorage.
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }
}
