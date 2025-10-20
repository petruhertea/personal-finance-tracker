package com.petruth.personal_finance_tracker.rest;

import com.petruth.personal_finance_tracker.dto.BudgetDTO;
import com.petruth.personal_finance_tracker.entity.Budget;
import com.petruth.personal_finance_tracker.security.SecurityUtil;
import com.petruth.personal_finance_tracker.service.BudgetService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetRestController {
    private final BudgetService budgetService;
    private final SecurityUtil securityUtil;

    public BudgetRestController(BudgetService budgetService, SecurityUtil securityUtil) {
        this.budgetService = budgetService;
        this.securityUtil = securityUtil;
    }

    @PostMapping
    public BudgetDTO createBudget(@RequestBody BudgetDTO budgetDTO) {
        // Ensure budget is created for current user
        Long currentUserId = securityUtil.getCurrentUserId();
        budgetDTO.setUserId(currentUserId);
        return budgetService.createBudget(budgetDTO);
    }

    @PutMapping("/{id}")
    public BudgetDTO updateBudget(@PathVariable Long id, @RequestBody BudgetDTO budgetDTO) {
        Long currentUserId = securityUtil.getCurrentUserId();

        Budget tempBudget = budgetService.findById(id);

        securityUtil.validateResourceOwnership(tempBudget.getUser().getId(), "Budget");
        budgetDTO.setUserId(currentUserId);
        return budgetService.updateBudget(id, budgetDTO);
    }

    @DeleteMapping("/{id}")
    public String deleteBudget(@PathVariable Long id) {
        Budget tempBudget = budgetService.findById(id);

        if(tempBudget==null){
            throw new RuntimeException("Budget id "+id+" not found");
        } else{
            securityUtil.validateResourceOwnership(tempBudget.getUser().getId(), "Budget");
            budgetService.deleteById(id);
        }
        return "Budget id -> "+id+" deleted successfully!";
    }
}

