package com.campusconnect.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.campusconnect.model.Role;
import com.campusconnect.model.User;
import com.campusconnect.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String SECRET = "a-test-secret-that-is-long-enough-1234567890";

    @Mock
    private UserRepository userRepository;

    private JwtService jwtService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtService = new JwtService(SECRET, "https://campusconnect.edu", 60);
        filter = new JwtAuthenticationFilter(jwtService, userRepository);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesTokenForCurrentAccountVersion() throws Exception {
        User user = user();
        String token = jwtService.generateToken(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        filter.doFilter(request(token), new MockHttpServletResponse(), (req, res) -> { });

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNotNull()
                .extracting(authentication -> authentication.getName())
                .isEqualTo("user-1");
    }

    @Test
    void rejectsTokenIssuedBeforePasswordReset() throws Exception {
        User accountAtIssue = user();
        String token = jwtService.generateToken(accountAtIssue);
        User accountAfterReset = user();
        accountAfterReset.setTokenVersion(1);
        when(userRepository.findById(accountAfterReset.getId())).thenReturn(Optional.of(accountAfterReset));

        filter.doFilter(request(token), new MockHttpServletResponse(), (req, res) -> { });

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private MockHttpServletRequest request(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return request;
    }

    private User user() {
        User user = new User();
        user.setId("user-1");
        user.setRole(Role.STUDENT);
        return user;
    }
}
