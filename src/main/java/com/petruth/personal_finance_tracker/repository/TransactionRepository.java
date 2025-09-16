package com.petruth.personal_finance_tracker.repository;

import com.petruth.personal_finance_tracker.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
    List<Transaction> findByUserId(int id);
}
