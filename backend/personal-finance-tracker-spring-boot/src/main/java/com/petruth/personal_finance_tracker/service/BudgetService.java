package com.petruth.personal_finance_tracker.service;

import com.petruth.personal_finance_tracker.dto.BudgetDTO;
import com.petruth.personal_finance_tracker.entity.Budget;

import java.util.List;

public interface BudgetService {
    BudgetDTO createBudget(BudgetDTO budgetDTO);
    List<BudgetDTO> findByUserId(Long userId);
    Budget findById(Long id);
    BudgetDTO updateBudget(Long id, BudgetDTO budgetDTO);
    void deleteById(Long id);
}
