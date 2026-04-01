package com.eatzy.common.util;

import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;

import com.eatzy.common.exception.IdInvalidException;

public class SecurityUtils {

    /**
     * Get the ID of the current logged-in user from the JWT token.
     * 
     * @return the ID of the current user, extracted from the "user" claim in the JWT.
     * @throws IdInvalidException if the user is unauthorized or the ID is missing.
     */
    public static Long getCurrentUserId() throws IdInvalidException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            Map<String, Object> userClaim = jwt.getClaim("user");
            if (userClaim != null && userClaim.get("id") != null) {
                return Long.valueOf(userClaim.get("id").toString());
            }
        }
        throw new IdInvalidException("Unauthorized or missing user id in token");
    }

    /**
     * Get the login/email of the current user.
     *
     * @return the login/email of the current user.
     */
    public static Optional<String> getCurrentUserLogin() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(extractPrincipal(securityContext.getAuthentication()));
    }

    private static String extractPrincipal(Authentication authentication) {
        if (authentication == null) {
            return null;
        } else if (authentication.getPrincipal() instanceof UserDetails springSecurityUser) {
            return springSecurityUser.getUsername();
        } else if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        } else if (authentication.getPrincipal() instanceof String s) {
            return s;
        }
        return null;
    }

    /**
     * Get the JWT of the current user.
     *
     * @return the JWT of the current user.
     */
    public static Optional<String> getCurrentUserJWT() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(securityContext.getAuthentication())
                .filter(authentication -> authentication.getCredentials() instanceof String)
                .map(authentication -> (String) authentication.getCredentials());
    }
}
