package com.petruth.personal_finance_tracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BudgetDTO {
    private Long id;
    private BigDecimal amount;
    private Long userId;
    private Long categoryId;
    private LocalDate startDate;
    private LocalDate endDate;

    public BudgetDTO(){

    }

    public BudgetDTO(Long id, BigDecimal amount, Long userId, Long categoryId,
                     LocalDate startDate, LocalDate endDate) {
        this.id = id;
        this.amount = amount;
        this.userId = userId;
        this.categoryId = categoryId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
