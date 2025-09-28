package com.petruth.personal_finance_tracker.utils;

import com.petruth.personal_finance_tracker.dto.BudgetDTO;
import com.petruth.personal_finance_tracker.entity.Budget;
import com.petruth.personal_finance_tracker.entity.Category;
import com.petruth.personal_finance_tracker.entity.User;
import org.springframework.stereotype.Component;

@Component
public class BudgetMapper {
    public BudgetDTO toBudgetDTO(Budget budget) {
        BudgetDTO budgetDTO = new BudgetDTO();
        budgetDTO.setId(budget.getId());
        budgetDTO.setAmount(budget.getAmount());
        budgetDTO.setUserId(budget.getUser().getId());
        budgetDTO.setCategoryId(budget.getCategory().getId());
        budgetDTO.setStartDate(budget.getStartDate());
        budgetDTO.setEndDate(budget.getEndDate());
        return budgetDTO;
    }

    public Budget toBudgetEntity(BudgetDTO budgetDTO, User user, Category category) {
        Budget budget = new Budget();
        budget.setId(budgetDTO.getId());
        budget.setAmount(budgetDTO.getAmount());
        budget.setUser(user);
        budget.setCategory(category);
        budget.setStartDate(budgetDTO.getStartDate());
        budget.setEndDate(budgetDTO.getEndDate());
        return budget;
    }
}

