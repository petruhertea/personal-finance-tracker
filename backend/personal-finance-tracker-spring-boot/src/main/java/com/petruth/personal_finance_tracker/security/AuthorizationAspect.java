/*
package com.petruth.personal_finance_tracker.security;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuthorizationAspect {

    private final SecurityUtil securityUtil;

    public AuthorizationAspect(SecurityUtil securityUtil) {
        this.securityUtil = securityUtil;
    }

    // Automatically validate userId in path variables
    @Before("execution(* com.petruth.personal_finance_tracker.rest..*(..)) && args(userId,..)")
    public void validateUserId(JoinPoint joinPoint, Long userId) {
        if (!securityUtil.isCurrentUser(userId)) {
            throw new SecurityException("Unauthorized access to user data");
        }
    }
}

 */
