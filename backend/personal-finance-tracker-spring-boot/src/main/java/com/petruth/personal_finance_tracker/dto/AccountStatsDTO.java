package com.petruth.personal_finance_tracker.dto;

import java.time.LocalDateTime;

public class AccountStatsDTO {
    private LocalDateTime memberSince;
    private long totalTransactions;
    private long totalBudgets;
    private LocalDateTime lastLogin;

    public AccountStatsDTO() {}

    public AccountStatsDTO(LocalDateTime memberSince, long totalTransactions,
                           long totalBudgets, LocalDateTime lastLogin) {
        this.memberSince = memberSince;
        this.totalTransactions = totalTransactions;
        this.totalBudgets = totalBudgets;
        this.lastLogin = lastLogin;
    }

    public LocalDateTime getMemberSince() {
        return memberSince;
    }

    public void setMemberSince(LocalDateTime memberSince) {
        this.memberSince = memberSince;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(long totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public long getTotalBudgets() {
        return totalBudgets;
    }

    public void setTotalBudgets(long totalBudgets) {
        this.totalBudgets = totalBudgets;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
}