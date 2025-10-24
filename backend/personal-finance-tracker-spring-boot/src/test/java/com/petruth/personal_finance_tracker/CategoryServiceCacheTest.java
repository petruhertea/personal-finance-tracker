package com.petruth.personal_finance_tracker;

import com.petruth.personal_finance_tracker.dto.CategoryDTO;
import com.petruth.personal_finance_tracker.entity.Category;
import com.petruth.personal_finance_tracker.repository.CategoryRepository;
import com.petruth.personal_finance_tracker.service.CategoryService;
import com.petruth.personal_finance_tracker.utils.CategoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CategoryServiceCacheTest {

    @Autowired
    private CategoryService categoryService;

    @SpyBean
    private CategoryRepository categoryRepository;

    @Autowired
    private CacheManager cacheManager;

    private final Long userId = 1L;

    @BeforeEach
    void setup() {
        cacheManager.getCache("categories").clear(); // asigură cache curat
    }

    @Test
    void shouldCacheCategoriesByUserId() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Food");

        when(categoryRepository.findByUserIsNull()).thenReturn(Collections.emptyList());
        when(categoryRepository.findByUserId(userId)).thenReturn(List.of(category));

        // prima apelare -> ar trebui să facă query real
        List<CategoryDTO> firstCall = categoryService.getAllCategoriesForUser(userId);
        assertThat(firstCall).hasSize(1);

        // a doua apelare -> ar trebui să vină din cache
        List<CategoryDTO> secondCall = categoryService.getAllCategoriesForUser(userId);
        assertThat(secondCall).hasSize(1);

        // repository-ul trebuie apelat o singură dată
        verify(categoryRepository, times(1)).findByUserId(userId);
    }

    @Test
    void shouldEvictCacheWhenCategoryIsSaved() {
        categoryService.getAllCategoriesForUser(userId); // populate cache

        CategoryDTO dto = new CategoryDTO();
        dto.setUserId(1L);
        dto.setName("Test Category");
        categoryService.saveFromDTO(dto);


        assertThat(Objects.requireNonNull(cacheManager.getCache("categories")).get(userId)).isNull();
    }
}
