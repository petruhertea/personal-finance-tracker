package com.petruth.personal_finance_tracker.rest;

import com.petruth.personal_finance_tracker.dto.TransactionDTO;
import com.petruth.personal_finance_tracker.entity.User;
import com.petruth.personal_finance_tracker.service.TransactionService;
import com.petruth.personal_finance_tracker.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserRestController {
    private final UserService userService;
    private final TransactionService transactionService;

    public UserRestController(UserService userService, TransactionService transactionService) {
        this.userService = userService;
        this.transactionService = transactionService;
    }

    @PostMapping
    public User createUser(@RequestBody User user){
        return userService.save(user);
    }

    @GetMapping("/{id}/transactions")
    public List<TransactionDTO> getUserTransactions(@PathVariable int id) {
        return transactionService.findByUserId(id);
    }
}
