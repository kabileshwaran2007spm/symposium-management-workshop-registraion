package com.campusconnect.security;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

import com.campusconnect.model.User;
import com.campusconnect.repository.UserRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authenticates requests carrying {@code Authorization: Bearer <JWT>}.
 *
 * <p>The authenticated principal is the user's id, and the role from the token
 * becomes a {@code ROLE_*} authority. A missing or invalid token is not an error
 * here: the request simply stays anonymous and the authorization rules decide
 * whether that is acceptable, which lets public endpoints keep working with a
 * stale token in the browser.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            if (!token.isEmpty() && SecurityContextHolder.getContext().getAuthentication() == null) {
                authenticate(request, token);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String token) {
        try {
            Jwt jwt = jwtService.decode(token);
            String userId = jwt.getSubject();
            String role = jwt.getClaimAsString("role");
            Object versionClaim = jwt.getClaims().get("ver");
            Integer tokenVersion = versionClaim instanceof Number number ? number.intValue() : null;

            if (userId == null || role == null || role.isBlank()) {
                log.debug("Rejecting token without subject or role claim");
                return;
            }

            User user = userRepository.findById(userId).orElse(null);
            if (user == null
                    || !user.getRole().name().equals(role)
                    || (tokenVersion != null ? tokenVersion : 0) != user.getTokenVersion()) {
                log.debug("Rejecting token for an unavailable or changed account");
                return;
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId, null, List.of(new SimpleGrantedAuthority(ROLE_PREFIX + role)));
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (JwtException | IllegalArgumentException ex) {
            // Invalid or expired token: stay anonymous and let the security
            // rules produce a 401 where authentication is actually required.
            SecurityContextHolder.clearContext();
            log.debug("Rejected JWT: {}", ex.getMessage());
        }
    }
}
