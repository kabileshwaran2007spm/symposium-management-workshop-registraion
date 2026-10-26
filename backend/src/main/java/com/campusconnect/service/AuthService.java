package com.campusconnect.service;

import java.time.Instant;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.campusconnect.dto.AuthResponse;
import com.campusconnect.dto.LoginRequest;
import com.campusconnect.dto.RegisterRequest;
import com.campusconnect.dto.UserResponse;
import com.campusconnect.exception.BusinessRuleException;
import com.campusconnect.exception.UnauthorizedException;
import com.campusconnect.model.Role;
import com.campusconnect.model.User;
import com.campusconnect.repository.UserRepository;
import com.campusconnect.security.GoogleIdTokenVerifier;
import com.campusconnect.security.JwtService;

/**
 * Account creation and authentication.
 *
 * <p>Passwords are hashed with BCrypt before they are stored, and a login failure
 * always reports the same message whether the email is unknown or the password is
 * wrong, so the endpoint cannot be used to discover which emails are registered.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String INVALID_CREDENTIALS = "Invalid email or password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            GoogleIdTokenVerifier googleIdTokenVerifier) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleIdTokenVerifier = googleIdTokenVerifier;
    }

    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        String studentId = request.studentId().trim().toUpperCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException("An account with this email already exists");
        }
        if (userRepository.existsByStudentIdIgnoreCase(studentId)) {
            throw new BusinessRuleException("An account with this student ID already exists");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setStudentId(studentId);
        user.setDepartment(request.department().trim());
        // Role is fixed here rather than taken from the request: public signup can
        // never mint an ORGANIZER or ADMIN account.
        user.setRole(Role.STUDENT);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setCreatedAt(Instant.now());

        User saved = userRepository.save(user);
        log.info("Registered new student account {}", saved.getEmail());
        return UserResponse.from(saved);
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }

        String token = jwtService.generateToken(user);
        return AuthResponse.of(token, jwtService.getExpirySeconds(), user);
    }

    public AuthResponse loginWithGoogle(String idToken) {
        GoogleIdTokenVerifier.GoogleIdentity identity = googleIdTokenVerifier.verify(idToken);
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(identity.email()))
                .orElseThrow(() -> new UnauthorizedException(
                        "No CampusConnect account is registered with this Google email. Create an account first."));

        return AuthResponse.of(
                jwtService.generateToken(user),
                jwtService.getExpirySeconds(),
                user);
    }

    /**
     * Logout is intentionally a no-op on the server.
     *
     * <p>Tokens are stateless and self-contained, so there is nothing to delete.
     * Revoking them early would require a token blacklist, which is not part of the
     * documented scope. The client discards the token, which is what actually ends
     * the session.
     */
    public void logout() {
        log.debug("Logout requested; stateless tokens are discarded client-side");
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
