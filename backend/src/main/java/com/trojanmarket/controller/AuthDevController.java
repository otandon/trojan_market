package com.trojanmarket.controller;

import com.trojanmarket.dto.AuthResponseDTO;
import com.trojanmarket.dto.DevLoginRequest;
import com.trojanmarket.entity.User;
import com.trojanmarket.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mock SSO endpoint for local development. Skips the real Shibboleth/SAML round-trip
 * and issues a JWT directly given an @usc.edu email. Gated by @Profile("dev") so it
 * is never registered in production.
 */
@RestController
@RequestMapping("/auth")
@Profile("dev")
public class AuthDevController {

    private final AuthService authService;

    public AuthDevController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/dev-login")
    public ResponseEntity<AuthResponseDTO> devLogin(@Valid @RequestBody DevLoginRequest req) {
        User stub = new User();
        stub.setEmail(req.getEmail() == null ? null : req.getEmail().trim());
        // handleSSOLogin enforces the @usc.edu domain check and creates the user if new.
        return ResponseEntity.ok(authService.handleSSOLogin(stub));
    }
}
