package com.petruth.personal_finance_tracker.scheduler;

import com.petruth.personal_finance_tracker.dto.BudgetWithSpending;
import com.petruth.personal_finance_tracker.entity.User;
import com.petruth.personal_finance_tracker.repository.UserRepository;
import com.petruth.personal_finance_tracker.service.BudgetService;
import com.petruth.personal_finance_tracker.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class BudgetAlertScheduler {

    private static final Logger logger = LoggerFactory.getLogger(BudgetAlertScheduler.class);

    private final BudgetService budgetService;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public BudgetAlertScheduler(BudgetService budgetService,
                                UserRepository userRepository,
                                EmailService emailService) {
        this.budgetService = budgetService;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    /**
     * Run every day at 8 AM
     * Cron format: second minute hour day month weekday
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void checkBudgetAlerts() {
        logger.info("🔔 Starting budget alert check at {}", LocalDate.now());

        // Get all users with verified email
        List<User> verifiedUsers = userRepository.findByEmailVerifiedTrue();
        logger.info("📧 Found {} users with verified emails", verifiedUsers.size());

        int alertsSent = 0;

        for (User user : verifiedUsers) {
            try {
                alertsSent += checkUserBudgets(user);
            } catch (Exception e) {
                logger.error("❌ Failed to check budgets for user {}: {}",
                        user.getUsername(), e.getMessage());
            }
        }

        logger.info("✅ Budget alert check completed. Sent {} alerts", alertsSent);
    }

    /**
     * Check all budgets for a specific user
     */
    private int checkUserBudgets(User user) {
        List<BudgetWithSpending> budgets = budgetService.getAllBudgetsWithSpending(user.getId());
        int alertsSent = 0;

        for (BudgetWithSpending budget : budgets) {
            // Only check active budgets (within date range)
            LocalDate now = LocalDate.now();
            if (now.isBefore(budget.startDate()) || now.isAfter(budget.endDate())) {
                continue; // Skip inactive budgets
            }

            // Check if alert should be sent
            if (budget.shouldSendAlert()) {
                sendBudgetAlert(user, budget);
                alertsSent++;
            }
        }

        return alertsSent;
    }

    /**
     * Send budget alert email
     */
    private void sendBudgetAlert(User user, BudgetWithSpending budget) {
        try {
            emailService.sendBudgetAlert(
                    user.getEmail(),
                    user.getUsername(),
                    budget.categoryName(),
                    budget.spent().doubleValue(),
                    budget.budgetAmount().doubleValue(),
                    budget.percentage(),
                    budget.getAlertType()
            );

            logger.info("📧 Sent {} alert to {} for category: {} ({}%)",
                    budget.getAlertType(), user.getUsername(),
                    budget.categoryName(), "%.1f".formatted(budget.percentage()));

        } catch (Exception e) {
            logger.error("❌ Failed to send budget alert to {}: {}",
                    user.getEmail(), e.getMessage());
        }
    }

    /**
     * Manual trigger for testing (can be called via endpoint)
     */
    public void triggerManualCheck() {
        logger.info("🔔 Manual budget alert check triggered");
        checkBudgetAlerts();
    }
}