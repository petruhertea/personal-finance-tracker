package com.petruth.personal_finance_tracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO that includes budget + calculated spending data
 */
public record BudgetWithSpending(
        Long id,
        String categoryName,
        Long categoryId,
        BigDecimal budgetAmount,
        BigDecimal spent,
        BigDecimal remaining,
        Double percentage,
        Boolean isOverBudget,
        Boolean nearThreshold,
        Integer alertThreshold,
        LocalDate startDate,
        LocalDate endDate
) {
    // Helper method to check if alert should be sent
    public boolean shouldSendAlert() {
        return nearThreshold || isOverBudget;
    }

    public String getAlertType() {
        if (isOverBudget) {
            return "OVER_BUDGET";
        } else if (nearThreshold) {
            return "WARNING";
        }
        return "NONE";
    }
}