package com.campusconnect.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Authentication and authorization rules.
 *
 * <p>The API is stateless: no session is created, and every protected request is
 * authenticated from its bearer token. Roles are enforced here at the path level
 * and again with {@code @PreAuthorize} on controllers, while resource ownership
 * is checked inside the services. Frontend route guards are a convenience only -
 * this class and the ownership checks are the real boundary.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String PRODUCTION_FRONTEND_ORIGIN =
            "https://symposium-management-workshop-registraion-lol3-94n7acls4.vercel.app";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final AuthenticationRateLimitFilter authenticationRateLimitFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AuthenticationRateLimitFilter authenticationRateLimitFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationRateLimitFilter = authenticationRateLimitFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public authentication endpoints.
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()

                        // Public discovery. "/api/events/*" matches exactly one
                        // path segment, so the organizer/admin sub-resources such
                        // as "/api/events/{id}/attendance" are deliberately NOT
                        // covered and still require authentication.
                        .requestMatchers(HttpMethod.GET, "/api/events").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/*").permitAll()

                        // Role rules at the path level. These run in the filter
                        // chain, before the request body is read or validated, so an
                        // unauthorized caller always gets 403 rather than a 400 from
                        // body parsing. The same rules are repeated as @PreAuthorize
                        // on the controllers - defence in depth, not a second source
                        // of truth.
                        //
                        // ORDER MATTERS: Spring Security uses the first matching
                        // rule, and "/api/events/**" would otherwise swallow
                        // "/api/events/{id}/register". The specific sub-resources
                        // must therefore be declared before the broad ones.
                        .requestMatchers(HttpMethod.POST, "/api/events/*/register").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.DELETE, "/api/events/*/register").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/events/*/attendance/**")
                        .hasAnyRole("ORGANIZER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/attendance/**")
                        .hasAnyRole("ORGANIZER", "ADMIN")

                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/organizer/**").hasAnyRole("ORGANIZER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/events").hasRole("ORGANIZER")
                        .requestMatchers(HttpMethod.PUT, "/api/events/**").hasRole("ORGANIZER")
                        .requestMatchers(HttpMethod.DELETE, "/api/events/**").hasRole("ORGANIZER")

                        // Anything left only needs a valid token.
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, ex) ->
                                writeError(response, HttpStatus.UNAUTHORIZED, "Authentication is required"))
                        .accessDeniedHandler((request, response, ex) ->
                                writeError(response, HttpStatus.FORBIDDEN,
                                        "You do not have permission to perform this action")))
                .addFilterBefore(authenticationRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Writes the standard error envelope for requests rejected before they reach a
     * controller. The payload is a fixed literal, so no user input reaches the
     * response body.
     */
    private static void writeError(HttpServletResponse response, HttpStatus status, String message)
            throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"success\":false,\"message\":\"" + message + "\",\"data\":null}");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS is restricted to the configured frontend origins. Wildcards are not
     * used, and credentials are allowed because the SPA sends a bearer token.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:5173}") String allowedOrigins) {

        List<String> origins = new ArrayList<>(Arrays.stream(allowedOrigins.split(","))
                .map(origin -> origin.trim())
                .filter(origin -> !origin.isEmpty())
                .toList());
        if (!origins.contains(PRODUCTION_FRONTEND_ORIGIN)) {
            origins.add(PRODUCTION_FRONTEND_ORIGIN);
        }

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of(
                HttpMethod.GET.name(), HttpMethod.POST.name(), HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(), HttpMethod.DELETE.name(), HttpMethod.OPTIONS.name()));
        configuration.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT,
                HttpHeaders.ORIGIN, "X-Requested-With"));
        // Lets the browser read the download filename for CSV exports.
        configuration.setExposedHeaders(List.of(HttpHeaders.CONTENT_DISPOSITION));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
