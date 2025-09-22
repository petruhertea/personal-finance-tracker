package com.petruth.personal_finance_tracker.service;

import com.petruth.personal_finance_tracker.dto.BudgetDTO;
import com.petruth.personal_finance_tracker.entity.Budget;
import com.petruth.personal_finance_tracker.entity.Category;
import com.petruth.personal_finance_tracker.entity.User;
import com.petruth.personal_finance_tracker.repository.BudgetRepository;
import com.petruth.personal_finance_tracker.repository.CategoryRepository;
import com.petruth.personal_finance_tracker.repository.UserRepository;
import com.petruth.personal_finance_tracker.utils.BudgetMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BudgetServiceImpl implements BudgetService{

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetMapper budgetMapper;

    public BudgetServiceImpl(BudgetRepository budgetRepository, UserRepository userRepository,
                             CategoryRepository categoryRepository, BudgetMapper budgetMapper) {
        this.budgetRepository = budgetRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.budgetMapper = budgetMapper;
    }

    @Override
    public BudgetDTO createBudget(BudgetDTO budgetDTO) {
        User user = userRepository.findById(budgetDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Category category = categoryRepository.findById(budgetDTO.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Budget budget = budgetMapper.toBudgetEntity(budgetDTO, user, category);
        return budgetMapper.toBudgetDTO(budgetRepository.save(budget));
    }

    @Override
    public List<BudgetDTO> findByUserId(Long userId) {
        return budgetRepository.findByUserId(userId)
                .stream().map(budgetMapper::toBudgetDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Budget findById(Long id) {
        return budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget with id -> " + id +" not found"));
    }

    @Override
    public BudgetDTO updateBudget(Long id, BudgetDTO budgetDTO) {
        Budget existing = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        existing.setAmount(budgetDTO.getAmount());
        existing.setStartDate(budgetDTO.getStartDate());
        existing.setEndDate(budgetDTO.getEndDate());

        return budgetMapper.toBudgetDTO(budgetRepository.save(existing));
    }

    @Override
    public void deleteById(Long id) {
        budgetRepository.deleteById(id);
    }
}
