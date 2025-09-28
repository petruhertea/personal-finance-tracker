package com.petruth.personal_finance_tracker.rest;

import com.petruth.personal_finance_tracker.dto.TransactionDTO;
import com.petruth.personal_finance_tracker.entity.Transaction;
import com.petruth.personal_finance_tracker.service.TransactionService;
import com.petruth.personal_finance_tracker.utils.TransactionMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/transactions")
public class TransactionRestController {

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    public TransactionRestController(TransactionService transactionService,
                                     TransactionMapper transactionMapper){
        this.transactionService = transactionService;
        this.transactionMapper = transactionMapper;
    }

    @PostMapping
    public TransactionDTO createTransaction(@RequestBody TransactionDTO transactionDTO){
        transactionDTO.setDate(LocalDateTime.now());
        Transaction newTransaction = transactionService.saveFromDTO(transactionDTO);

        return transactionMapper.toTransactionDTO(newTransaction);
    }

    @PutMapping
    public TransactionDTO updateTransaction(@RequestBody TransactionDTO transactionDTO){
        Transaction transaction = transactionService.findById(transactionDTO.getId());
        transactionDTO.setDate(transaction.getDate());

        Transaction saved = transactionService.saveFromDTO(transactionDTO);

        return transactionMapper.toTransactionDTO(saved);
    }

    @DeleteMapping("/{id}")
    public String deleteTransaction(@PathVariable Long id){
        Transaction tempTransaction = transactionService.findById(id);

        if (tempTransaction == null){
            throw new RuntimeException("Transaction with the id -> "+ id + " was not found");
        }
        else{
            transactionService.deleteById(id);
        }

        return "The transaction with the id -> " + id + " was deleted";
    }
}
