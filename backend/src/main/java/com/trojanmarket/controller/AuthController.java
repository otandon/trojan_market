package com.trojanmarket.controller;

import com.trojanmarket.dto.AuthResponseDTO;
import com.trojanmarket.dto.SSOCallbackRequest;
import com.trojanmarket.entity.User;
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

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/sso/callback")
    public ResponseEntity<AuthResponseDTO> handleSSOCallback(@Valid @RequestBody SSOCallbackRequest request) {
        User userFromSSO = parseSSOToken(request.getToken());
        AuthResponseDTO response = authService.handleSSOLogin(userFromSSO);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponseDTO> getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new ForbiddenException("Not authenticated");
        }
        return ResponseEntity.ok(AuthResponseDTO.builder()
                .userID(principal.getUserID())
                .username(principal.getUsername())
                .email(principal.getEmail())
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        // Stateless JWT — server-side logout clears the security context.
        // The client must also discard the token from localStorage.
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    private User parseSSOToken(String token) {
        // TODO: integrate with USC SSO IdP. Parse the SAML assertion or OIDC ID token
        //       returned from USC's identity provider and extract the verified email
        //       (eduPersonPrincipalName / mail) and any other attributes. Until that
        //       integration is wired up, the token body is treated as the email so
        //       end-to-end auth flow can be exercised in dev.
        User user = new User();
        user.setEmail(token == null ? null : token.trim());
        return user;
    }
}
