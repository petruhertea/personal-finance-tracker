package com.petruth.personal_finance_tracker.rest;

import com.petruth.personal_finance_tracker.dto.BudgetDTO;
import com.petruth.personal_finance_tracker.entity.Budget;
import com.petruth.personal_finance_tracker.service.BudgetService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetRestController {
    private final BudgetService budgetService;

    public BudgetRestController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping
    public BudgetDTO createBudget(@RequestBody BudgetDTO budgetDTO) {
        return budgetService.createBudget(budgetDTO);
    }

    @PutMapping("/{id}")
    public BudgetDTO updateBudget(@PathVariable Long id, @RequestBody BudgetDTO budgetDTO) {
        return budgetService.updateBudget(id, budgetDTO);
    }

    @DeleteMapping("/{id}")
    public String deleteBudget(@PathVariable Long id) {
        Budget tempBudget = budgetService.findById(id);

        if(tempBudget==null){
            throw new RuntimeException("Budget id "+id+" not found");
        } else{
            budgetService.deleteById(id);
        }
        return "Budget id -> "+id+" deleted successfully!";
    }
}

