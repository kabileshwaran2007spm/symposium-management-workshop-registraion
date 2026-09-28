package com.campusconnect.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.campusconnect.exception.UnauthorizedException;
import com.campusconnect.model.Role;

/**
 * Reads the current caller out of the security context.
 *
 * <p>Services use this for ownership checks (an organizer may only touch their own
 * events), which is the second half of authorization: the role gate on the
 * controller says "an organizer may call this", and these helpers answer "may
 * <em>this</em> organizer".
 */
public final class SecurityUtils {

    private static final String ROLE_PREFIX = "ROLE_";

    private SecurityUtils() {
    }

    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /** @throws UnauthorizedException when the request is not authenticated. */
    public static String currentUserId() {
        Authentication authentication = getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof String userId)) {
            throw new UnauthorizedException("Authentication is required");
        }
        return userId;
    }

    /** @return the caller's role, or {@code null} when anonymous. */
    public static Role currentRole() {
        Authentication authentication = getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return null;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(ROLE_PREFIX))
                .map(authority -> authority.substring(ROLE_PREFIX.length()))
                .findFirst()
                .map(SecurityUtils::parseRoleOrNull)
                .orElse(null);
    }

    public static boolean isAdmin() {
        return currentRole() == Role.ADMIN;
    }

    private static Role parseRoleOrNull(String value) {
        try {
            return Role.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
