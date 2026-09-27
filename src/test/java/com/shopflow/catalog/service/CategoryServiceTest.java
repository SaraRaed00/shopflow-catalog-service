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
import java.util.List;
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
    //
    @Test
    void findAll_shouldReturnAllCategories() {
        Category category = new Category();

        CategoryResponse response = new CategoryResponse(1L, "Electronics", "electronics", null,  Collections.emptyList(), null, null);

        when(categoryRepository.findAll()).thenReturn(List.of(category));
        when(categoryMapper.toResponse(category)).thenReturn(response);

        List<CategoryResponse> result = categoryService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).slug()).isEqualTo("electronics");
    }

    @Test
    void update_shouldModifyCategory_whenNoParent() {
        Category category = new Category();
        CreateCategoryRequest request = new CreateCategoryRequest("new-slug", "New Name", null);
        CategoryResponse response =new CategoryResponse(1L, "New Name", "new-slug", null, Collections.emptyList(), null, null);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.findByParentId(any())).thenReturn(Collections.emptyList());
        when(categoryMapper.toResponse(category)).thenReturn(response);

        CategoryResponse result = categoryService.update(1L, request);

        assertThat(result.name()).isEqualTo("New Name");
        assertThat(category.getName()).isEqualTo("New Name");
    }

    @Test
    void update_shouldSetNewParent_whenParentIdProvided() {
        Category category = new Category();
        category.setId(1L);

        Category newParent = new Category();
        newParent.setId(5L);

        CreateCategoryRequest request = new CreateCategoryRequest("slug", "Name", 5L);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(newParent));
        when(categoryRepository.findByParentId(any())).thenReturn(Collections.emptyList());
        when(categoryMapper.toResponse(category)).thenReturn(
            new CategoryResponse(1L, "Name", "slug", 5L, Collections.emptyList(), null, null));

        categoryService.update(1L, request);

        assertThat(category.getParent()).isEqualTo(newParent);
    }

    @Test
    void update_shouldThrowConflict_whenParentIsSelf() {
        Category category = new Category();
        category.setId(1L);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        CreateCategoryRequest request = new CreateCategoryRequest("Name", "slug", 1L);

        assertThatThrownBy(() -> categoryService.update(1L, request))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("cannot be its own parent");
    }

}
