package com.petruth.personal_finance_tracker.service;

import com.petruth.personal_finance_tracker.dto.BudgetDTO;
import com.petruth.personal_finance_tracker.dto.BudgetWithSpending;
import com.petruth.personal_finance_tracker.entity.Budget;
import com.petruth.personal_finance_tracker.entity.Category;
import com.petruth.personal_finance_tracker.entity.Transaction;
import com.petruth.personal_finance_tracker.entity.User;
import com.petruth.personal_finance_tracker.repository.BudgetRepository;
import com.petruth.personal_finance_tracker.repository.CategoryRepository;
import com.petruth.personal_finance_tracker.repository.TransactionRepository;
import com.petruth.personal_finance_tracker.repository.UserRepository;
import com.petruth.personal_finance_tracker.utils.BudgetMapper;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@CacheConfig(cacheNames = "budgets")
public class BudgetServiceImpl implements BudgetService{

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetMapper budgetMapper;

    public BudgetServiceImpl(BudgetRepository budgetRepository, UserRepository userRepository,
                             CategoryRepository categoryRepository, TransactionRepository transactionRepository,
                             BudgetMapper budgetMapper) {
        this.budgetRepository = budgetRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.budgetMapper = budgetMapper;
    }

    @Override
    @CacheEvict(allEntries = true)
    public BudgetDTO createBudget(BudgetDTO budgetDTO) {
        User user = userRepository.findById(budgetDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Category category = categoryRepository.findById(budgetDTO.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Budget budget = budgetMapper.toBudgetEntity(budgetDTO, user, category);
        return budgetMapper.toBudgetDTO(budgetRepository.save(budget));
    }

    @Override
    @Cacheable(key = "'user-' + #userId + '-budgets'")
    public List<BudgetDTO> findByUserId(Long userId) {
        return budgetRepository.findByUserId(userId)
                .stream().map(budgetMapper::toBudgetDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(key = "'budget-' + #id")
    public Budget findById(Long id) {
        return budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget with id -> " + id +" not found"));
    }

    @Override
    @CacheEvict(allEntries = true)
    public BudgetDTO updateBudget(Long id, BudgetDTO budgetDTO) {
        Budget existing = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        existing.setAmount(budgetDTO.getAmount());
        existing.setStartDate(budgetDTO.getStartDate());
        existing.setEndDate(budgetDTO.getEndDate());

        return budgetMapper.toBudgetDTO(budgetRepository.save(existing));
    }

    @Override
    @CacheEvict(allEntries = true)
    public void deleteById(Long id) {
        budgetRepository.deleteById(id);
    }

    /**
     * ✅ Calculate budget with actual spending
     */
    @Override
    @Transactional(readOnly = true)
    public BudgetWithSpending calculateBudgetWithSpending(Budget budget) {
        LocalDate now = LocalDate.now();

        // If budget is not active, return with zero spending
        if (now.isBefore(budget.getStartDate()) || now.isAfter(budget.getEndDate())) {
            return createInactiveBudgetStatus(budget);
        }

        // Get all EXPENSE transactions for this category within budget period
        List<Transaction> transactions = transactionRepository.findByUserIdAndCategoryAndDateBetween(
                budget.getUser().getId(),
                budget.getCategory(),
                budget.getStartDate().atStartOfDay(),
                budget.getEndDate().atTime(23, 59, 59)
        );

        // Sum up only EXPENSE transactions
        BigDecimal spent = transactions.stream()
                .filter(tx -> tx.getType() == Transaction.TransactionType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate remaining and percentage
        BigDecimal remaining = budget.getAmount().subtract(spent);

        double percentage = BigDecimal.ZERO.equals(budget.getAmount())
                ? 0.0
                : spent.divide(budget.getAmount(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();

        // Determine alert status
        boolean isOverBudget = percentage > 100;
        boolean nearThreshold = percentage >= budget.getAlertThreshold();

        return new BudgetWithSpending(
                budget.getId(),
                budget.getCategory().getName(),
                budget.getCategory().getId(),
                budget.getAmount(),
                spent,
                remaining,
                percentage,
                isOverBudget,
                nearThreshold,
                budget.getAlertThreshold(),
                budget.getStartDate(),
                budget.getEndDate()
        );
    }

    /**
     * ✅ Get all budgets with spending for a user
     */
    @Override
    @Transactional(readOnly = true)
    public List<BudgetWithSpending> getAllBudgetsWithSpending(Long userId) {
        List<Budget> budgets = budgetRepository.findByUserId(userId);

        return budgets.stream()
                .map(this::calculateBudgetWithSpending)
                .collect(Collectors.toList());
    }

    /**
     * Helper: Create inactive budget status (no spending calculation)
     */
    private BudgetWithSpending createInactiveBudgetStatus(Budget budget) {
        return new BudgetWithSpending(
                budget.getId(),
                budget.getCategory().getName(),
                budget.getCategory().getId(),
                budget.getAmount(),
                BigDecimal.ZERO,
                budget.getAmount(),
                0.0,
                false,
                false,
                budget.getAlertThreshold(),
                budget.getStartDate(),
                budget.getEndDate()
        );
    }
}
