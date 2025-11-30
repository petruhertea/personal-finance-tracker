package com.petruth.personal_finance_tracker.controller;

import com.petruth.personal_finance_tracker.dto.BudgetWithSpending;
import com.petruth.personal_finance_tracker.dto.CategoryDTO;
import com.petruth.personal_finance_tracker.dto.TransactionDTO;
import com.petruth.personal_finance_tracker.dto.UserResponse;
import com.petruth.personal_finance_tracker.entity.Transaction;
import com.petruth.personal_finance_tracker.entity.User;
import com.petruth.personal_finance_tracker.security.SecurityUtil;
import com.petruth.personal_finance_tracker.service.BudgetService;
import com.petruth.personal_finance_tracker.service.CategoryService;
import com.petruth.personal_finance_tracker.service.TransactionService;
import com.petruth.personal_finance_tracker.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final TransactionService transactionService;
    private final UserService userService;
    private final CategoryService categoryService;
    private final BudgetService budgetService;
    private final SecurityUtil securityUtil;

    public UserController(TransactionService transactionService,
                          UserService userService,
                          CategoryService categoryService,
                          BudgetService budgetService,
                          SecurityUtil securityUtil) {
        this.transactionService = transactionService;
        this.userService = userService;
        this.categoryService = categoryService;
        this.budgetService = budgetService;
        this.securityUtil = securityUtil;
    }

    // Validate that the requesting user matches the userId in path
    private void validateUserAccess(Long userId) {
        if (!securityUtil.isCurrentUser(userId)) {
            throw new SecurityException("Unauthorized access to user data");
        }
    }

    @GetMapping("/{userId}/transactions")
    public List<TransactionDTO> getUserTransactions(
            @PathVariable Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount
    ) {
        validateUserAccess(userId);
        return transactionService.findByUserId(userId.intValue(), type, fromDate, toDate,
                categoryId, minAmount, maxAmount);
    }

    @GetMapping("/{userId}/transactions/chart")
    public List<TransactionDTO> getUserChartTransactions(@PathVariable Long userId,
                                                         @RequestParam Transaction.TransactionType type) {
        validateUserAccess(userId);
        return transactionService.findByUserIdAndTypeOrderByDate(userId.intValue(), type);
    }

    @GetMapping("/{userId}/categories")
    public List<CategoryDTO> getCategoriesByUser(@PathVariable Long userId) {
        validateUserAccess(userId);
        return categoryService.getAllCategoriesForUser(userId);
    }

    @GetMapping("/{userId}/budgets")
    public List<BudgetWithSpending> getBudgetsByUser(@PathVariable Long userId) {
        validateUserAccess(userId);
        return budgetService.getAllBudgetsWithSpending(userId);
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser() {
        String username = securityUtil.getCurrentUsername();
        User user = userService.findByUsername(username);

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getEmailVerified()
        );
    }
}