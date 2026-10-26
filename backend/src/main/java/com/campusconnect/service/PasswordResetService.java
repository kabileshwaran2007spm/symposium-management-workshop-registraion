package com.campusconnect.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import com.campusconnect.exception.ApiException;
import com.campusconnect.model.PasswordResetToken;
import com.campusconnect.model.User;
import com.campusconnect.repository.UserRepository;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;
    private final PasswordEncoder passwordEncoder;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String frontendUrl;
    private final String mailFrom;
    private final String smtpHost;
    private final long expiryMinutes;

    public PasswordResetService(
            UserRepository userRepository,
            MongoTemplate mongoTemplate,
            PasswordEncoder passwordEncoder,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.frontend.url:http://localhost:5173}") String frontendUrl,
            @Value("${app.mail.from:}") String mailFrom,
            @Value("${spring.mail.host:}") String smtpHost,
            @Value("${app.password-reset.expiry-minutes:30}") long expiryMinutes) {
        this.userRepository = userRepository;
        this.mongoTemplate = mongoTemplate;
        this.passwordEncoder = passwordEncoder;
        this.mailSenderProvider = mailSenderProvider;
        this.frontendUrl = frontendUrl;
        this.mailFrom = mailFrom;
        this.smtpHost = smtpHost;
        this.expiryMinutes = expiryMinutes;
    }

    /**
     * Always returns the same response at the controller, whether or not an account
     * exists, so this endpoint cannot be used to enumerate registered emails.
     */
    public void requestReset(String email) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        userRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(user -> sendResetLink(user));
    }

    public void resetPassword(String rawToken, String newPassword) {
        Instant now = Instant.now();
        PasswordResetToken resetToken = mongoTemplate.findAndRemove(
                Query.query(Criteria.where("tokenHash").is(hashToken(rawToken))
                        .and("expiresAt").gt(now)),
                PasswordResetToken.class);

        if (resetToken == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "This reset link is invalid or expired. Request a new one.");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                        "This reset link is invalid or expired. Request a new one."));
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
    }

    private void sendResetLink(User user) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || smtpHost.isBlank() || mailFrom.isBlank()) {
            log.error("Password reset email was requested but SMTP_HOST, SMTP_FROM, or a mail sender is not configured");
            return;
        }

        String rawToken = newToken();
        Instant now = Instant.now();

        mongoTemplate.remove(Query.query(Criteria.where("userId").is(user.getId())), PasswordResetToken.class);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUserId(user.getId());
        resetToken.setTokenHash(hashToken(rawToken));
        resetToken.setExpiresAt(now.plus(expiryMinutes, ChronoUnit.MINUTES));
        mongoTemplate.save(resetToken);

        String resetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/reset-password")
                .queryParam("token", rawToken)
                .build()
                .toUriString();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(user.getEmail());
        message.setSubject("Reset your CampusConnect password");
        message.setText("We received a request to reset your CampusConnect password.\n\n"
                + "Use this one-time link within " + expiryMinutes + " minutes:\n"
                + resetUrl + "\n\n"
                + "If you did not request a reset, you can ignore this email.");

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            mongoTemplate.remove(Query.query(Criteria.where("tokenHash").is(resetToken.getTokenHash())),
                    PasswordResetToken.class);
            log.error("Could not send a CampusConnect password reset email", ex);
        }
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
