package com.petruth.personal_finance_tracker.service;

import com.petruth.personal_finance_tracker.dto.BudgetDTO;
import com.petruth.personal_finance_tracker.dto.BudgetWithSpending;
import com.petruth.personal_finance_tracker.entity.Budget;

import java.util.List;

public interface BudgetService {
    BudgetDTO createBudget(BudgetDTO budgetDTO);
    List<BudgetDTO> findByUserId(Long userId);
    Budget findById(Long id);
    BudgetDTO updateBudget(Long id, BudgetDTO budgetDTO);
    void deleteById(Long id);

    // ✅ New methods for spending calculation
    BudgetWithSpending calculateBudgetWithSpending(Budget budget);
    List<BudgetWithSpending> getAllBudgetsWithSpending(Long userId);
}
