package com.petruth.personal_finance_tracker.service;

import com.petruth.personal_finance_tracker.dto.TransactionDTO;
import com.petruth.personal_finance_tracker.entity.Transaction;

import java.util.List;

public interface TransactionService {
    List<TransactionDTO> findByUserId(int id);
    Transaction save(Transaction transaction);
    void deleteById(Integer id);
    Transaction findById(Integer id);
    Transaction saveFromDTO(TransactionDTO transactionDTO);
}
