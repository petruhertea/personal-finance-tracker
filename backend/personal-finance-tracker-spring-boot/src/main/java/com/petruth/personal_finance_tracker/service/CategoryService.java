package com.petruth.personal_finance_tracker.service;

import com.petruth.personal_finance_tracker.dto.CategoryDTO;
import com.petruth.personal_finance_tracker.entity.Category;

import java.util.List;

public interface CategoryService {
    Category findById(Long id);
    List<CategoryDTO> findByUserId(Long userId);
    Category saveFromDTO(CategoryDTO dto);
    void deleteById(Long id);
    List<CategoryDTO> getAllCategoriesForUser(Long userId);
    List<CategoryDTO> findByUserIsNull();
    Category findByName(String name);
}

