package com.shopflow.catalog.service;

import com.shopflow.catalog.domain.exception.ConflictException;
import com.shopflow.catalog.domain.exception.NotFoundException;
import com.shopflow.catalog.domain.model.Category;
import com.shopflow.catalog.mapper.CategoryMapper;
import com.shopflow.catalog.repository.CategoryRepository;
import com.shopflow.catalog.repository.ProductRepository;
import com.shopflow.catalog.web.dto.CategoryResponse;
import com.shopflow.catalog.web.dto.CreateCategoryRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void create_shouldSaveAndReturnResponse_whenNoParent() {
        CreateCategoryRequest request = new CreateCategoryRequest("toys", "Toys", null);
        Category saved = new Category();
        CategoryResponse mapped = new CategoryResponse(1L, "Toys", "toys", null, Collections.emptyList(), null, null);

        when(categoryRepository.save(any(Category.class))).thenReturn(saved);
        when(categoryMapper.toResponse(saved)).thenReturn(mapped);
        when(categoryRepository.findByParentId(null)).thenReturn(Collections.emptyList());   // <-- null, not 1L

        CategoryResponse result = categoryService.create(request);

        assertThat(result.slug()).isEqualTo("toys");
        assertThat(result.children()).isEmpty();
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void create_shouldThrowNotFound_whenParentDoesNotExist() {
        CreateCategoryRequest request = new CreateCategoryRequest("phones", "Phones", 999L);
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.create(request))
            .isInstanceOf(NotFoundException.class);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void delete_shouldThrowConflict_whenCategoryHasProducts() {
        Category category = new Category();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete(1L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("products");

        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void delete_shouldThrowConflict_whenCategoryHasChildren() {
        Category category = new Category();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.existsByCategoryId(1L)).thenReturn(false);
        when(categoryRepository.findByParentId(1L)).thenReturn(java.util.List.of(new Category()));

        assertThatThrownBy(() -> categoryService.delete(1L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("child");

        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void delete_shouldSucceed_whenNoProductsOrChildren() {
        Category category = new Category();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.existsByCategoryId(1L)).thenReturn(false);
        when(categoryRepository.findByParentId(1L)).thenReturn(Collections.emptyList());

        categoryService.delete(1L);

        verify(categoryRepository).delete(category);
    }

    @Test
    void findById_shouldThrowNotFound_whenMissing() {
        when(categoryRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findById(42L))
            .isInstanceOf(NotFoundException.class);
    }
}
