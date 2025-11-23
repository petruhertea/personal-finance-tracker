package com.petruth.personal_finance_tracker.service;

import com.petruth.personal_finance_tracker.entity.User;
import com.petruth.personal_finance_tracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class EmailVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationService.class);

    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${app.email-verification.token-expiry-hours:24}")
    private int tokenExpiryHours;

    public EmailVerificationService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    /**
     * Generate and send verification email
     */
    @Transactional
    public void sendVerificationEmail(User user) {
        // Generate unique token
        String token = UUID.randomUUID().toString();

        // Set token and expiry
        user.setEmailVerificationToken(token);
        user.setEmailVerificationTokenExpiry(LocalDateTime.now().plusHours(tokenExpiryHours));
        user.setEmailVerified(false);

        userRepository.save(user);

        // Send email
        emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), token);

        logger.info("📧 Verification email sent to: {}", user.getEmail());
    }

    /**
     * Verify email with token
     */
    @Transactional
    public boolean verifyEmail(String token) {
        Optional<User> userOpt = userRepository.findByEmailVerificationToken(token);

        if (userOpt.isEmpty()) {
            logger.warn("⚠️ Invalid verification token: {}", token);
            return false;
        }

        User user = userOpt.get();

        // Check if token expired
        if (!user.isEmailVerificationTokenValid()) {
            logger.warn("⚠️ Expired verification token for user: {}", user.getEmail());
            return false;
        }

        // Mark as verified
        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationTokenExpiry(null);

        userRepository.save(user);

        logger.info("✅ Email verified for user: {}", user.getEmail());
        return true;
    }

    /**
     * Resend verification email
     */
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getEmailVerified()) {
            throw new RuntimeException("Email already verified");
        }

        sendVerificationEmail(user);
    }
}
