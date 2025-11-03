package com.petruth.personal_finance_tracker.repository;

import com.petruth.personal_finance_tracker.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@CrossOrigin("http://localhost:4200")
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserId(Long userId);
    long countByUserId(Long userId);
}
