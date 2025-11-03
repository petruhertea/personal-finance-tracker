package com.petruth.personal_finance_tracker.repository;

import com.petruth.personal_finance_tracker.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@CrossOrigin("http://localhost:4200")
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {
    List<Transaction> findByUserIdOrderByDateDesc(int id);
    List<Transaction> findByUserIdAndTypeOrderByDate(int id, Transaction.TransactionType type);
    long countByUserId(Long userId);
}
