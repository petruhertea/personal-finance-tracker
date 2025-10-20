package com.petruth.personal_finance_tracker.rest;

import com.petruth.personal_finance_tracker.dto.TransactionDTO;
import com.petruth.personal_finance_tracker.entity.Transaction;
import com.petruth.personal_finance_tracker.security.SecurityUtil;
import com.petruth.personal_finance_tracker.service.TransactionService;
import com.petruth.personal_finance_tracker.utils.TransactionMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/transactions")
public class TransactionRestController {

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;
    private final SecurityUtil securityUtil;

    public TransactionRestController(TransactionService transactionService,
                                     TransactionMapper transactionMapper,
                                     SecurityUtil securityUtil) {
        this.transactionService = transactionService;
        this.transactionMapper = transactionMapper;
        this.securityUtil = securityUtil;
    }

    @PostMapping
    public TransactionDTO createTransaction(@RequestBody TransactionDTO transactionDTO) {
        Long userId = securityUtil.getCurrentUserId(); // extragi din token
        transactionDTO.setUserId(userId);
        transactionDTO.setDate(LocalDateTime.now());

        Transaction newTransaction = transactionService.saveFromDTO(transactionDTO);
        return transactionMapper.toTransactionDTO(newTransaction);
    }

    @PutMapping
    public TransactionDTO updateTransaction(@RequestBody TransactionDTO transactionDTO) {
        Long userId = securityUtil.getCurrentUserId();

        Transaction existing = transactionService.findById(transactionDTO.getId());

        transactionDTO.setDate(existing.getDate());

        securityUtil.validateResourceOwnership(
                existing.getUser().getId(),
                "Transaction"
        );

        transactionDTO.setUserId(userId);

        Transaction saved = transactionService.saveFromDTO(transactionDTO);
        return transactionMapper.toTransactionDTO(saved);
    }

    @DeleteMapping("/{id}")
    public String deleteTransaction(@PathVariable Long id) {
        Transaction transaction = transactionService.findById(id);
        securityUtil.validateResourceOwnership(
                transaction.getUser().getId(),
                "Transaction"
        );

        transactionService.deleteById(id);
        return "The transaction with id " + id + " was deleted";
    }
}
