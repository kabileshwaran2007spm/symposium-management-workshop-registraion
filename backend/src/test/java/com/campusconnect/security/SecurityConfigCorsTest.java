package com.campusconnect.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class SecurityConfigCorsTest {

    @Test
    void productionFrontendRemainsAllowedWhenCorsOriginsAreOverridden() {
        SecurityConfig config = new SecurityConfig(
                mock(JwtAuthenticationFilter.class),
                mock(AuthenticationRateLimitFilter.class));
        CorsConfigurationSource source = config.corsConfigurationSource("http://localhost:5173");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/events");
        request.setServletPath("/api/events");
        CorsConfiguration cors = source.getCorsConfiguration(request);

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOrigins()).contains(
                "http://localhost:5173",
                "https://symposium-management-workshop-registraion-lol3-94n7acls4.vercel.app");
        assertThat(cors.getAllowCredentials()).isNotEqualTo(true);
        assertThat(cors.checkOrigin("https://untrusted.example")).isNull();
        assertThat(cors.getAllowedMethods()).contains(HttpMethod.OPTIONS.name(), HttpMethod.GET.name());
        assertThat(cors.getAllowedHeaders()).contains(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE);
    }
}
