// TokenCleanupScheduler.java - Cleanup expired tokens daily
package com.petruth.personal_finance_tracker.security;

import com.petruth.personal_finance_tracker.service.RefreshTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TokenCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(TokenCleanupScheduler.class);
    private final RefreshTokenService refreshTokenService;

    public TokenCleanupScheduler(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Run cleanup every day at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupExpiredTokens() {
        logger.info("Starting cleanup of expired refresh tokens");
        refreshTokenService.cleanupExpiredTokens();
        logger.info("Expired refresh tokens cleanup completed");
    }
}