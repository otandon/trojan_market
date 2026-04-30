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
        // TODO(USC SSO): Wire up the real Shibboleth/SAML2 (or OIDC) integration here.
        //   The full flow is:
        //     1. Frontend hits GET /auth/sso/start, which 302-redirects the browser to
        //        https://shibboleth.usc.edu/idp/profile/SAML2/Redirect/SSO with an
        //        AuthnRequest signed by our SP keypair (entityID: app.usc.sso.entity-id).
        //     2. User authenticates against USC + completes Duo MFA at the IdP.
        //     3. IdP HTTP-POSTs a signed SAMLResponse back to this callback URL
        //        (app.usc.sso.callback-url).
        //     4. We must verify the signature against the IdP metadata
        //        (app.usc.sso.idp-metadata-url), check audience/conditions/replay,
        //        then extract the `mail` / `eduPersonPrincipalName` attribute as the
        //        verified @usc.edu email.
        //   Until that is wired up the dev environment should use POST /auth/dev-login
        //   (see AuthDevController, gated by @Profile("dev")) instead of this endpoint.
        User user = new User();
        user.setEmail(token == null ? null : token.trim());
        return user;
    }
}
