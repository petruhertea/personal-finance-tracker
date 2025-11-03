// RefreshTokenService.java
package com.petruth.personal_finance_tracker.service;

import com.petruth.personal_finance_tracker.entity.RefreshToken;
import com.petruth.personal_finance_tracker.entity.User;
import com.petruth.personal_finance_tracker.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserService userService;

    // 7 days in milliseconds
    private static final long REFRESH_TOKEN_VALIDITY = 7 * 24 * 60 * 60 * 1000L;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository,
                               UserService userService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userService = userService;
    }

    /**
     * Create a new refresh token for a user
     */
    @Override
    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        User user = userService.findById(userId);

        // Delete any existing refresh tokens for this user
        refreshTokenRepository.deleteByUser(user);

        // Generate new refresh token
        String tokenValue = UUID.randomUUID().toString();
        Instant expiryDate = Instant.now().plusMillis(REFRESH_TOKEN_VALIDITY);

        RefreshToken refreshToken = new RefreshToken(user, tokenValue, expiryDate);
        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Verify and return refresh token
     */
    @Override
    public Optional<RefreshToken> verifyRefreshToken(String token) {
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByToken(token);

        if (refreshToken.isEmpty()) {
            return Optional.empty();
        }

        RefreshToken rt = refreshToken.get();

        // Check if token is revoked
        if (rt.isRevoked()) {
            return Optional.empty();
        }

        // Check if token is expired
        if (rt.isExpired()) {
            refreshTokenRepository.delete(rt);
            return Optional.empty();
        }

        return Optional.of(rt);
    }

    /**
     * Revoke a refresh token
     */
    @Transactional
    @Override
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    /**
     * Revoke all refresh tokens for a user (logout from all devices)
     */
    @Transactional
    @Override
    public void revokeAllUserTokens(Long userId) {
        User user = userService.findById(userId);
        refreshTokenRepository.deleteByUser(user);
    }

    /**
     * Clean up expired tokens (can be scheduled)
     */
    @Transactional
    @Override
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens();
    }
}