package com.petruth.personal_finance_tracker.rest;

import com.petruth.personal_finance_tracker.dto.TransactionDTO;
import com.petruth.personal_finance_tracker.dto.UserResponse;
import com.petruth.personal_finance_tracker.entity.Transaction;
import com.petruth.personal_finance_tracker.entity.User;
import com.petruth.personal_finance_tracker.security.CustomUserDetails;
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
    public UserRestController(TransactionService transactionService,
                              UserService userService
    ) {
        this.transactionService = transactionService;
        this.userService = userService;
    }

    @GetMapping("/{id}/transactions")
    public List<TransactionDTO> getUserTransactions(@PathVariable int id) {
        return transactionService.findByUserId(id);
    }

    @GetMapping("/{id}/transactions/chart")
    public List<TransactionDTO> getUserChartTransactions(@PathVariable int id,
                                                         @RequestParam Transaction.TransactionType type) {
        return transactionService.findByUserIdAndTypeOrderByDate(id, type);
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
