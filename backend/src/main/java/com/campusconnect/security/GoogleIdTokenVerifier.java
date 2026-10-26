package com.campusconnect.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.stereotype.Component;

import com.campusconnect.exception.ApiException;

@Component
public class GoogleIdTokenVerifier {

    private static final String GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final String GOOGLE_ISSUER = "https://accounts.google.com";

    private final JwtDecoder decoder;
    private final String clientId;

    public GoogleIdTokenVerifier(@Value("${app.google.client-id:}") String clientId) {
        this.clientId = clientId == null ? "" : clientId.trim();
        NimbusJwtDecoder googleDecoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWK_SET_URI).build();
        OAuth2TokenValidator<Jwt> googleClaims = jwt -> {
            List<String> audience = jwt.getAudience();
            boolean audienceMatches = audience != null && audience.contains(this.clientId);
            boolean emailVerified = Boolean.TRUE.equals(jwt.getClaim("email_verified"));
            boolean hasEmail = jwt.getClaimAsString("email") != null
                    && !jwt.getClaimAsString("email").isBlank();
            if (audienceMatches && emailVerified && hasEmail) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Google identity claims are invalid", null));
        };
        googleDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(GOOGLE_ISSUER),
                googleClaims));
        this.decoder = googleDecoder;
    }

    public GoogleIdentity verify(String idToken) {
        if (clientId.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Google sign-in is not configured.");
        }

        try {
            Jwt jwt = decoder.decode(idToken);
            String email = jwt.getClaimAsString("email");
            String name = jwt.getClaimAsString("name");
            if (email == null || email.isBlank()) {
                throw new BadJwtException("Google identity does not include an email");
            }
            return new GoogleIdentity(email, name == null ? "" : name);
        } catch (JwtException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Google sign-in could not be verified.");
        }
    }

    public record GoogleIdentity(String email, String name) {
    }
}
