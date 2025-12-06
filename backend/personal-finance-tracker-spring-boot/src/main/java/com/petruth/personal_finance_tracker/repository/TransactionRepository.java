package com.petruth.personal_finance_tracker.repository;

import com.petruth.personal_finance_tracker.entity.Category;
import com.petruth.personal_finance_tracker.entity.Transaction;
import com.petruth.personal_finance_tracker.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.time.LocalDateTime;
import java.util.List;

@CrossOrigin("http://localhost:4200")
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {
    List<Transaction> findByUserIdOrderByDateDesc(int id);
    List<Transaction> findByUserIdAndTypeOrderByDate(int id, Transaction.TransactionType type);
    long countByUserId(Long userId);

    boolean existsByAmountHashAndUser(String hash, User user);

    // ✅ New method: Find user's transactions by category and date range
    List<Transaction> findByUserIdAndCategoryAndDateBetween(
            Long userId,
            Category category,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    // Add paginated version
    Page<Transaction> findAll(Specification<Transaction> spec, Pageable pageable);
}
