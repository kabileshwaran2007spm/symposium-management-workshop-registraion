package com.campusconnect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

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
 * Registration and login behaviour, including the rule that public signup can
 * never produce a privileged account.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private GoogleIdTokenVerifier googleIdTokenVerifier;

    /** A real encoder, so the stored hash is genuinely verifiable. */
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService, googleIdTokenVerifier);
    }

    private RegisterRequest request() {
        return new RegisterRequest("Arun Kumar", "arun@example.com", "CS2026001", "Password@123", "CSE");
    }

    @Test
    @DisplayName("registration stores a BCrypt hash and never the raw password")
    void registrationHashesThePassword() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByStudentIdIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user-1");
            return user;
        });

        UserResponse response = authService.register(request());

        assertThat(response.email()).isEqualTo("arun@example.com");
        assertThat(response.role()).isEqualTo(Role.STUDENT);
        assertThat(response.id()).isEqualTo("user-1");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());

        String stored = saved.getValue().getPassword();
        assertThat(stored).isNotEqualTo("Password@123");
        assertThat(stored).startsWith("$2");
        assertThat(passwordEncoder.matches("Password@123", stored)).isTrue();
    }

    @Test
    @DisplayName("a role supplied on the signup payload is ignored, so nobody can self-promote")
    void registrationCannotEscalateRole() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByStudentIdIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // RegisterRequest has no role component at all; the type system is the
        // first line of defence. This asserts the resulting role explicitly.
        UserResponse response = authService.register(request());

        assertThat(response.role()).isEqualTo(Role.STUDENT);
    }

    @Test
    @DisplayName("a duplicate email is rejected")
    void duplicateEmailIsRejected() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("email already exists");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("a duplicate student id is rejected")
    void duplicateStudentIdIsRejected() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByStudentIdIgnoreCase(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("student ID already exists");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("valid credentials produce a token and the caller's profile")
    void validLoginReturnsToken() {
        User user = new User();
        user.setId("user-1");
        user.setName("Arun Kumar");
        user.setEmail("arun@example.com");
        user.setStudentId("CS2026001");
        user.setDepartment("CSE");
        user.setRole(Role.STUDENT);
        user.setPassword(passwordEncoder.encode("Password@123"));

        when(userRepository.findByEmailIgnoreCase("arun@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("signed-jwt");
        when(jwtService.getExpirySeconds()).thenReturn(86400L);

        AuthResponse response = authService.login(new LoginRequest("arun@example.com", "Password@123"));

        assertThat(response.token()).isEqualTo("signed-jwt");
        assertThat(response.role()).isEqualTo(Role.STUDENT);
        assertThat(response.expiresInSeconds()).isEqualTo(86400L);
    }

    @Test
    @DisplayName("a wrong password is rejected without revealing that the account exists")
    void invalidPasswordIsRejected() {
        User user = new User();
        user.setPassword(passwordEncoder.encode("Password@123"));
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("arun@example.com", "WrongPassword")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    @DisplayName("an unknown email produces the same message as a wrong password")
    void unknownEmailIsIndistinguishableFromBadPassword() {
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "Password@123")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    @DisplayName("Google login returns a session for an existing verified-email account")
    void googleLoginReturnsExistingAccount() {
        User user = new User();
        user.setId("user-1");
        user.setName("Arun Kumar");
        user.setEmail("arun@example.com");
        user.setStudentId("CS2026001");
        user.setDepartment("CSE");
        user.setRole(Role.STUDENT);

        when(googleIdTokenVerifier.verify("google-id-token"))
                .thenReturn(new GoogleIdTokenVerifier.GoogleIdentity("ARUN@example.com", "Arun Kumar"));
        when(userRepository.findByEmailIgnoreCase("arun@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("signed-jwt");
        when(jwtService.getExpirySeconds()).thenReturn(86400L);

        AuthResponse response = authService.loginWithGoogle("google-id-token");

        assertThat(response.token()).isEqualTo("signed-jwt");
        assertThat(response.email()).isEqualTo("arun@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Google login does not create accounts for unregistered emails")
    void googleLoginDoesNotCreateAnAccount() {
        when(googleIdTokenVerifier.verify("google-id-token"))
                .thenReturn(new GoogleIdTokenVerifier.GoogleIdentity("new@example.com", "New User"));
        when(userRepository.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.loginWithGoogle("google-id-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Create an account first");

        verify(userRepository, never()).save(any(User.class));
        verify(jwtService, never()).generateToken(any(User.class));
    }
}
