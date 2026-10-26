package com.campusconnect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.campusconnect.exception.ApiException;
import com.campusconnect.model.PasswordResetToken;
import com.campusconnect.model.User;
import com.campusconnect.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(userRepository, mongoTemplate, passwordEncoder,
                mailSenderProvider, "https://campusconnect.example", "noreply@campusconnect.example",
                "smtp.example", 30);
    }

    @Test
    void unknownEmailDoesNotCreateATokenOrSendMail() {
        when(userRepository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());

        service.requestReset(" Unknown@example.com ");

        verify(mongoTemplate, never()).save(any(PasswordResetToken.class));
        verify(mailSenderProvider, never()).getIfAvailable();
    }

    @Test
    void requestedResetStoresOnlyTokenHashAndEmailsTheRawToken() {
        User user = new User();
        user.setId("user-1");
        user.setEmail("student@example.com");
        JavaMailSender mailSender = mock(JavaMailSender.class);
        when(userRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(user));
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);

        service.requestReset(" Student@example.com ");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mongoTemplate).save(tokenCaptor.capture());
        verify(mailSender).send(messageCaptor.capture());

        String resetUrl = messageCaptor.getValue().getText().lines()
                .filter(line -> line.contains("/reset-password?token="))
                .findFirst()
                .orElseThrow();
        String rawToken = resetUrl.substring(resetUrl.indexOf("token=") + "token=".length());

        assertThat(rawToken).hasSize(43);
        assertThat(tokenCaptor.getValue().getTokenHash()).hasSize(64).isNotEqualTo(rawToken);
        assertThat(tokenCaptor.getValue().getUserId()).isEqualTo("user-1");
        assertThat(tokenCaptor.getValue().getExpiresAt()).isNotNull();
    }

    @Test
    void resetConsumesTokenAndStoresBcryptPassword() {
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUserId("user-1");
        resetToken.setTokenHash("hash");
        User user = new User();
        user.setId("user-1");
        when(mongoTemplate.findAndRemove(any(Query.class), eq(PasswordResetToken.class))).thenReturn(resetToken);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        service.resetPassword("one-time-token", "NewPassword123");

        assertThat(passwordEncoder.matches("NewPassword123", user.getPassword())).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void invalidOrExpiredTokenIsRejected() {
        when(mongoTemplate.findAndRemove(any(Query.class), eq(PasswordResetToken.class))).thenReturn(null);

        assertThatThrownBy(() -> service.resetPassword("expired-token", "NewPassword123"))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("invalid or expired");

        verify(userRepository, never()).save(any(User.class));
    }
}
