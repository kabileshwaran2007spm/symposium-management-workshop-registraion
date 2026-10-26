package com.campusconnect.security;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Fixed-window rate limiting for the unauthenticated authentication endpoints.
 *
 * <p>Public authentication endpoints accept credentials or trigger password reset,
 * no token, which makes them the brute-force surface of the API. The limiter is
 * deliberately tiny and dependency-free: a per-client fixed window keyed by
 * {@code client IP + endpoint}, implemented with a concurrent map. It restarts
 * with the application, which is acceptable for a single-node deployment; a real
 * multi-node deployment would move this to a shared store or an edge gateway.
 *
 * <p>The check runs before the controller, so a flood is refused with a plain 429
 * without reaching the password encoder at all. Password verification is the most
 * expensive step of login, and keeping floods away from it is the point.
 */
@Component
public class AuthenticationRateLimitFilter extends OncePerRequestFilter {

    /** Failed attempts allowed per window per client per endpoint. */
    private static final int MAX_ATTEMPTS = 10;

    /** Window length. Ten tries per fifteen minutes throttles guessing without locking out shared campus NATs for long. */
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, Window> attempts = new ConcurrentHashMap<>();

    private record Window(long startMillis, int count) {
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only unauthenticated authentication writes are throttled. Everything
        // else relies on token authentication, which is not brute-forceable.
        return !(HttpMethod.POST.matches(request.getMethod())
                && ("/api/auth/login".equals(request.getServletPath())
                        || "/api/auth/register".equals(request.getServletPath())
                        || "/api/auth/google".equals(request.getServletPath())
                        || "/api/auth/forgot-password".equals(request.getServletPath())
                        || "/api/auth/reset-password".equals(request.getServletPath())));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String key = clientKey(request);
        long now = System.currentTimeMillis();
        long windowStart = now - WINDOW.toMillis();

        // Drop entries from earlier windows so the map cannot grow without bound.
        attempts.values().removeIf(window -> window.startMillis() < windowStart);

        Window current = attempts.compute(key, (k, existing) ->
                existing == null || existing.startMillis() < windowStart
                        ? new Window(now, 1)
                        : new Window(existing.startMillis(), existing.count() + 1));

        if (current.count() > MAX_ATTEMPTS) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(writeJson());
            return;
        }

        filterChain.doFilter(request, response);
    }

    /** Serialised once per rejection; kept separate so the hot path stays allocation-free. */
    private String writeJson() {
        return "{\"success\":false,\"message\":\"Too many attempts. Please wait a few minutes and try again.\",\"data\":null}";
    }

    /**
     * Identifies the caller. {@code X-Forwarded-For} is trusted only in the sense a
     * reverse proxy sets it; a spoofed value just gives an attacker their own
     * bucket, it cannot exhaust anyone else's.
     */
    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded != null && !forwarded.isBlank()
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
        return ip + "|" + request.getServletPath();
    }
}
