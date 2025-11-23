package com.petruth.personal_finance_tracker.utils;

import com.petruth.personal_finance_tracker.dto.CategoryDTO;
import com.petruth.personal_finance_tracker.entity.Category;
import com.petruth.personal_finance_tracker.entity.User;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryDTO toCategoryDTO(Category category) {
        return new CategoryDTO(
                category.getId(),
                category.getName(),
                category.getUser() != null ? category.getUser().getId() : null,
                category.getIcon(),
                category.getColor()
        );
    }

    public Category toCategory(CategoryDTO categoryDTO, User user) {
        return new Category(
                categoryDTO.getName(),
                categoryDTO.getIcon(),
                categoryDTO.getColor(),
                user
        );
    }
}

