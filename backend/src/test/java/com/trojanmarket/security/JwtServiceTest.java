package com.trojanmarket.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JwtService}.
 *
 * Maps to the test plan's authentication white-box section — the token must be
 * issued correctly and must reject tampered tokens.
 */
class JwtServiceTest {

    private static final String SECRET = "test-only-secret-must-be-at-least-32-bytes-long-yes";

    @Test
    void issuedTokenRoundtripsToSameClaims() {
        JwtService svc = new JwtService(SECRET, 60_000L, "trojan-market");

        String token = svc.issueToken(42, "ttrojan@usc.edu", "ttrojan");
        Claims claims = svc.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("email", String.class)).isEqualTo("ttrojan@usc.edu");
        assertThat(claims.get("username", String.class)).isEqualTo("ttrojan");
        assertThat(claims.getIssuer()).isEqualTo("trojan-market");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void tokenIssuedWithDifferentSecretFailsVerification() {
        JwtService issuer = new JwtService(SECRET, 60_000L, "trojan-market");
        JwtService verifier = new JwtService(
                "different-secret-also-at-least-32-bytes-long-but-other", 60_000L, "trojan-market");

        String token = issuer.issueToken(1, "x@usc.edu", "x");

        assertThatThrownBy(() -> verifier.parseToken(token))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void tamperedTokenFailsVerification() {
        JwtService svc = new JwtService(SECRET, 60_000L, "trojan-market");
        String token = svc.issueToken(1, "x@usc.edu", "x");
        // Flip a character in the signature (last segment) to simulate tampering.
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("a") ? "b" : "a");

        assertThatThrownBy(() -> svc.parseToken(tampered))
                .isInstanceOf(Exception.class);
    }

    @Test
    void garbageInputThrows() {
        JwtService svc = new JwtService(SECRET, 60_000L, "trojan-market");
        assertThatThrownBy(() -> svc.parseToken("not.a.jwt"))
                .isInstanceOf(Exception.class);
    }
}
