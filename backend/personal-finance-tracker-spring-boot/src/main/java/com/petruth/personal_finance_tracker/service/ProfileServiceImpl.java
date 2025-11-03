package com.petruth.personal_finance_tracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petruth.personal_finance_tracker.dto.*;
import com.petruth.personal_finance_tracker.entity.User;
import com.petruth.personal_finance_tracker.repository.BudgetRepository;
import com.petruth.personal_finance_tracker.repository.TransactionRepository;
import com.petruth.personal_finance_tracker.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ProfileServiceImpl implements ProfileService{

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final CategoryService categoryService;
    private final TransactionService transactionService;
    private final BudgetService budgetService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final ObjectMapper objectMapper;

    public ProfileServiceImpl(UserRepository userRepository,
                          TransactionRepository transactionRepository,
                          BudgetRepository budgetRepository,
                          CategoryService categoryService,
                          TransactionService transactionService,
                          BudgetService budgetService,
                          PasswordEncoder passwordEncoder,
                          RefreshTokenService refreshTokenService,
                          ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.categoryService = categoryService;
        this.transactionService = transactionService;
        this.budgetService = budgetService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.objectMapper = objectMapper;
    }

    /**
     * Update user email with password verification
     */
    @Transactional
    @Override
    public UserResponse updateEmail(Long userId, String newEmail, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // Check if email is already taken
        if (userRepository.existsByEmail(newEmail)) {
            throw new RuntimeException("Email already in use");
        }

        user.setEmail(newEmail);
        User updated = userRepository.save(user);

        return new UserResponse(updated.getId(), updated.getUsername(), updated.getEmail());
    }

    /**
     * Change user password
     */
    @Transactional
    @Override
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Validate new password
        if (newPassword.length() < 8) {
            throw new RuntimeException("New password must be at least 8 characters");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Revoke all refresh tokens for security
        refreshTokenService.revokeAllUserTokens(userId);
    }

    /**
     * Get account statistics
     */
    @Override
    public AccountStatsDTO getAccountStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        long totalTransactions = transactionRepository.countByUserId(userId);
        long totalBudgets = budgetRepository.countByUserId(userId);

        return new AccountStatsDTO(
                user.getCreatedAt() != null ? user.getCreatedAt() : LocalDateTime.now(),
                totalTransactions,
                totalBudgets,
                user.getLastLogin() != null ? user.getLastLogin() : LocalDateTime.now()
        );
    }

    /**
     * Export all user data (GDPR compliance)
     */
    @Override
    public UserDataExportDTO exportUserData(Long userId) {
        // Get user info
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        UserResponse userResponse = new UserResponse(user.getId(), user.getUsername(), user.getEmail());

        // Get all transactions
        var transactions = transactionService.findByUserId(userId.intValue(),
                null, null, null, null, null, null);

        // Get all budgets
        var budgets = budgetService.findByUserId(userId);

        // Get all categories
        var categories = categoryService.findByUserId(userId);

        // Get stats
        var stats = getAccountStats(userId);

        return new UserDataExportDTO(userResponse, transactions, budgets, categories, stats);
    }

    /**
     * Delete user account permanently
     */
    @Transactional
    @Override
    public void deleteAccount(Long userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // Revoke all refresh tokens
        refreshTokenService.revokeAllUserTokens(userId);

        // Delete user (cascade will handle related data)
        userRepository.delete(user);
    }

    /**
     * Update last login timestamp
     */
    @Transactional
    @Override
    public void updateLastLogin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
    }
}
