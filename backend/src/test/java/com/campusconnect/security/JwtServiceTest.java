package com.campusconnect.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

import com.campusconnect.model.Role;
import com.campusconnect.model.User;

class JwtServiceTest {

    private static final String SECRET = "a-test-secret-that-is-long-enough-1234567890";
    private static final String ISSUER = "https://campusconnect.edu";

    private JwtService service() {
        return new JwtService(SECRET, ISSUER, 60);
    }

    private User user() {
        User user = new User();
        user.setId("user-1");
        user.setName("Arun Kumar");
        user.setEmail("arun@example.com");
        user.setRole(Role.STUDENT);
        return user;
    }

    @Test
    @DisplayName("a token round-trips and carries the id, role and profile claims")
    void tokenRoundTrips() {
        JwtService jwtService = service();

        String token = jwtService.generateToken(user());
        Jwt decoded = jwtService.decode(token);

        assertThat(decoded.getSubject()).isEqualTo("user-1");
        assertThat(decoded.getClaimAsString("role")).isEqualTo("STUDENT");
        assertThat(decoded.getClaimAsString("name")).isEqualTo("Arun Kumar");
        assertThat(decoded.getClaimAsString("email")).isEqualTo("arun@example.com");
        assertThat(decoded.getIssuer().toString()).isEqualTo(ISSUER);
        assertThat(decoded.getExpiresAt()).isAfter(decoded.getIssuedAt());
    }

    @Test
    @DisplayName("the token exposes a three-part JWS")
    void tokenHasThreeParts() {
        String token = service().generateToken(user());
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("a tampered token is rejected")
    void tamperedTokenIsRejected() {
        JwtService jwtService = service();
        String token = jwtService.generateToken(user());

        // Flip a character in the signature segment.
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("A") ? "B" : "A");

        assertThatThrownBy(() -> jwtService.decode(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("a token signed with a different secret is rejected")
    void tokenFromAnotherSecretIsRejected() {
        String foreignToken = new JwtService("a-completely-different-secret-value-9876543210",
                ISSUER, 60).generateToken(user());

        assertThatThrownBy(() -> service().decode(foreignToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("garbage is rejected rather than throwing something unexpected")
    void garbageIsRejected() {
        assertThatThrownBy(() -> service().decode("not-a-jwt"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("startup fails when no secret is configured, rather than signing with a default")
    void missingSecretFailsFast() {
        assertThatThrownBy(() -> new JwtService("", ISSUER, 60))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET is not set");
    }

    @Test
    @DisplayName("a secret too short for HS256 is rejected")
    void shortSecretIsRejected() {
        assertThatThrownBy(() -> new JwtService("too-short", ISSUER, 60))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 characters");
    }

    @Test
    @DisplayName("the configured expiry is reported to the client")
    void expiryIsReported() {
        assertThat(service().getExpirySeconds()).isEqualTo(3600L);
    }
}
