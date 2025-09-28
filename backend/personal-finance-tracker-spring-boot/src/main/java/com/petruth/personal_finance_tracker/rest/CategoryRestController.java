package com.petruth.personal_finance_tracker.rest;

import com.petruth.personal_finance_tracker.dto.CategoryDTO;
import com.petruth.personal_finance_tracker.entity.Category;
import com.petruth.personal_finance_tracker.service.CategoryService;
import com.petruth.personal_finance_tracker.utils.CategoryMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryRestController {

    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;

    public CategoryRestController(CategoryService categoryService,
                                  CategoryMapper categoryMapper) {
        this.categoryService = categoryService;
        this.categoryMapper = categoryMapper;
    }

    @GetMapping
    public List<CategoryDTO> getCategories(){
        return categoryService.findByUserIsNull();
    }

    @GetMapping("/user/{userId}")
    public List<CategoryDTO> getCategoriesByUser(@PathVariable Long userId) {
        return categoryService.getAllCategoriesForUser(userId);
    }

    @PostMapping
    public CategoryDTO createCategory(@RequestBody CategoryDTO dto) {
        Category saved = categoryService.saveFromDTO(dto);
        return categoryMapper.toCategoryDTO(saved);
    }

    @DeleteMapping("/{id}")
    public void deleteCategory(@PathVariable Long id) {
        categoryService.deleteById(id);
    }
}
