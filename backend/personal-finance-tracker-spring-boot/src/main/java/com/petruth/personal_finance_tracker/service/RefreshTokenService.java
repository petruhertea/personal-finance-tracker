package com.petruth.personal_finance_tracker.service;

import com.petruth.personal_finance_tracker.entity.RefreshToken;

import java.util.Optional;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(Long userId);
    Optional<RefreshToken> verifyRefreshToken(String token);
    void revokeRefreshToken(String token);
    void revokeAllUserTokens(Long userId);
    void cleanupExpiredTokens();
}
