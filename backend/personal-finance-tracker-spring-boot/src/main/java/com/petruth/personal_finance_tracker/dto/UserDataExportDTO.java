package com.petruth.personal_finance_tracker.dto;

import java.util.List;

public class UserDataExportDTO {
    private UserResponse user;
    private List<TransactionDTO> transactions;
    private List<BudgetDTO> budgets;
    private List<CategoryDTO> categories;
    private AccountStatsDTO stats;

    public UserDataExportDTO() {}

    public UserDataExportDTO(UserResponse user, List<TransactionDTO> transactions,
                             List<BudgetDTO> budgets, List<CategoryDTO> categories,
                             AccountStatsDTO stats) {
        this.user = user;
        this.transactions = transactions;
        this.budgets = budgets;
        this.categories = categories;
        this.stats = stats;
    }

    // Getters and Setters
    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }

    public List<TransactionDTO> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<TransactionDTO> transactions) {
        this.transactions = transactions;
    }

    public List<BudgetDTO> getBudgets() {
        return budgets;
    }

    public void setBudgets(List<BudgetDTO> budgets) {
        this.budgets = budgets;
    }

    public List<CategoryDTO> getCategories() {
        return categories;
    }

    public void setCategories(List<CategoryDTO> categories) {
        this.categories = categories;
    }

    public AccountStatsDTO getStats() {
        return stats;
    }

    public void setStats(AccountStatsDTO stats) {
        this.stats = stats;
    }
}