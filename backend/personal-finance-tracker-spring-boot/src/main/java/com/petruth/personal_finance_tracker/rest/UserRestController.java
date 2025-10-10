package com.petruth.personal_finance_tracker.rest;

import com.petruth.personal_finance_tracker.dto.BudgetDTO;
import com.petruth.personal_finance_tracker.dto.CategoryDTO;
import com.petruth.personal_finance_tracker.dto.TransactionDTO;
import com.petruth.personal_finance_tracker.dto.UserResponse;
import com.petruth.personal_finance_tracker.entity.Transaction;
import com.petruth.personal_finance_tracker.entity.User;
import com.petruth.personal_finance_tracker.security.CustomUserDetails;
import com.petruth.personal_finance_tracker.service.BudgetService;
import com.petruth.personal_finance_tracker.service.CategoryService;
import com.petruth.personal_finance_tracker.service.TransactionService;
import com.petruth.personal_finance_tracker.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserRestController {

    private final TransactionService transactionService;
    private final UserService userService;
    private final CategoryService categoryService;
    private final BudgetService budgetService;

    public UserRestController(TransactionService transactionService,
                              UserService userService,
                              CategoryService categoryService,
                              BudgetService budgetService
    ) {
        this.transactionService = transactionService;
        this.userService = userService;
        this.categoryService = categoryService;
        this.budgetService = budgetService;
    }

    @GetMapping("/{userId}/transactions")
    public List<TransactionDTO> getUserTransactions(
            @PathVariable int userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount
    ) {
        return transactionService.findByUserId(userId, type, fromDate, toDate,
                categoryId, minAmount, maxAmount);
    }

    @GetMapping("/{userId}/categories")
    public List<CategoryDTO> getCategoriesByUser(@PathVariable Long userId) {
        return categoryService.getAllCategoriesForUser(userId);
    }

    @GetMapping("/{userId}/transactions/chart")
    public List<TransactionDTO> getUserChartTransactions(@PathVariable int userId,
                                                         @RequestParam Transaction.TransactionType type) {
        return transactionService.findByUserIdAndTypeOrderByDate(userId, type);
    }

    @GetMapping("/{userId}/budgets")
    public List<BudgetDTO> getBudgetsByUser(@PathVariable Long userId) {
        return budgetService.findByUserId(userId);
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser(Authentication authentication) {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userService.findByUsername(username);


        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail()
        );

        return userResponse;
    }
}
