package com.petruth.personal_finance_tracker.security;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {

    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal();
        }
        throw new SecurityException("No authenticated user found");
    }

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof Claims) {
            Claims claims = (Claims) authentication.getDetails();
            return claims.get("userId", Long.class);
        }
        throw new SecurityException("No authenticated user found");
    }

    public boolean isCurrentUser(Long userId) {
        try {
            return getCurrentUserId().equals(userId);
        } catch (Exception e) {
            return false;
        }
    }

    // NEW: Validate that the current user owns a resource
    public void validateResourceOwnership(Long resourceOwnerId, String resourceType) {
        Long currentUserId = getCurrentUserId();
        if (!currentUserId.equals(resourceOwnerId)) {
            throw new SecurityException(
                    String.format("User %d is not authorized to access %s owned by user %d",
                            currentUserId, resourceType, resourceOwnerId)
            );
        }
    }
}
