package com.campusconnect.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import com.campusconnect.model.User;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Issues and verifies HS256 JSON Web Tokens.
 *
 * <p>This uses Spring Security's own JOSE module rather than a third-party JWT
 * library, so the whole authentication stack stays on documented dependencies.
 *
 * <p>The secret is read from the environment. There is no fallback: a missing or
 * too-short secret fails application startup rather than silently signing tokens
 * with a guessable key.
 */
@Service
public class JwtService {

    /** HS256 needs a key of at least 256 bits. */
    private static final int MIN_SECRET_BYTES = 32;

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_VERSION = "ver";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Duration expiry;
    private final String issuer;

    public JwtService(
            @Value("${app.jwt.secret:}") String secret,
            @Value("${app.jwt.issuer:https://campusconnect.edu}") String issuer,
            @Value("${app.jwt.expiry-minutes:1440}") long expiryMinutes) {

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET is not set. Configure the JWT_SECRET environment variable with at least "
                            + MIN_SECRET_BYTES + " characters before starting CampusConnect.");
        }

        byte[] keyBytes = secret.trim().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least " + MIN_SECRET_BYTES + " characters long (got "
                            + keyBytes.length + ").");
        }

        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        // Signature and expiry are verified here. Issuer is pinned separately in
        // decode(), because the secret-key decoder builder does not expose an
        // issuer validator directly.
        this.decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        this.expiry = Duration.ofMinutes(expiryMinutes);
        this.issuer = issuer;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        // Claims are limited to identity and authorization. Profile fields such as
        // name and email deliberately stay out of the token: they are available
        // from the database, and carrying them only widens what a leaked token
        // exposes. The login response already carries the profile the UI needs.
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plus(expiry))
                .subject(user.getId())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_TOKEN_VERSION, user.getTokenVersion())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /**
     * Verifies signature, expiry and issuer.
     *
     * <p>The issuer check is explicit: a token signed with the correct secret but
     * minted by a different issuer (for example a misconfigured sibling environment
     * sharing the signing key) is rejected rather than trusted.
     *
     * @throws org.springframework.security.oauth2.jwt.JwtException when the token
     *         is invalid, expired, or from an untrusted issuer; callers treat that
     *         as unauthenticated.
     */
    public Jwt decode(String token) {
        Jwt jwt = decoder.decode(token);

        if (jwt.getIssuer() == null || !issuer.equals(jwt.getIssuer().toString())) {
            throw new org.springframework.security.oauth2.jwt.BadJwtException("Untrusted token issuer");
        }
        return jwt;
    }

    public long getExpirySeconds() {
        return expiry.toSeconds();
    }
}
