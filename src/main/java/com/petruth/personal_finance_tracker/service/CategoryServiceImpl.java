package com.petruth.personal_finance_tracker.service;

import com.petruth.personal_finance_tracker.dto.CategoryDTO;
import com.petruth.personal_finance_tracker.entity.Category;
import com.petruth.personal_finance_tracker.entity.User;
import com.petruth.personal_finance_tracker.repository.CategoryRepository;
import com.petruth.personal_finance_tracker.utils.CategoryMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final UserService userService;

    public CategoryServiceImpl(CategoryRepository categoryRepository,
                               CategoryMapper categoryMapper,
                               UserService userService) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
        this.userService = userService;
    }

    @Override
    public List<CategoryDTO> getAllCategoriesForUser(Long userId) {
        List<Category> predefined = categoryRepository.findByUserIsNull(); // categorii globale
        List<Category> userCategories = categoryRepository.findByUserId(userId); // categorii personalizate

        List<Category> combined = new ArrayList<>();
        combined.addAll(predefined);
        combined.addAll(userCategories);

        return combined.stream()
                .map(categoryMapper::toCategoryDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryDTO> findByUserIsNull() {
        List<Category> predefined = categoryRepository.findByUserIsNull();

        return predefined.stream()
                .map(categoryMapper::toCategoryDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
    }

    @Override
    public List<CategoryDTO> findByUserId(Long userId) {
        return categoryRepository.findByUserId(userId)
                .stream()
                .map(categoryMapper::toCategoryDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Category saveFromDTO(CategoryDTO dto) {
        User user = userService.findById(dto.getUserId());
        Category category = categoryMapper.toCategory(dto, user);
        return categoryRepository.save(category);
    }

    @Override
    public void deleteById(Long id) {
        categoryRepository.deleteById(id);
    }
}
