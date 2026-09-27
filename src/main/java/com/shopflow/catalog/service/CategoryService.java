package com.shopflow.catalog.service;

import com.shopflow.catalog.domain.exception.ConflictException;
import com.shopflow.catalog.domain.exception.NotFoundException;
import com.shopflow.catalog.domain.model.Category;
import com.shopflow.catalog.mapper.CategoryMapper;
import com.shopflow.catalog.repository.CategoryRepository;
import com.shopflow.catalog.repository.ProductRepository;
import com.shopflow.catalog.web.dto.CategoryResponse;
import com.shopflow.catalog.web.dto.CreateCategoryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;

    @Transactional
    @CacheEvict(value = "CategoryTree" , allEntries = true)
    public CategoryResponse create(CreateCategoryRequest request){
        Category category = new Category();
        category.setName(request.name());
        category.setSlug(request.slug());
        if(request.parentId()!= null){
            Category parent = categoryRepository.findById(request.parentId())
                .orElseThrow( () -> new NotFoundException("NO EXISTING CATEGORY WITH THIS ID", "PARENT ID IS INCORRECT: "+ request.parentId()));
            category.setParent(parent);
        }
        Category saved = categoryRepository.save(category);
        return categoryWithChildren(saved);
    }

    private CategoryResponse categoryWithChildren(Category category){
        List<Category> children= categoryRepository.findByParentId(category.getId());
        List<CategoryResponse> actualChildren = children.stream().map(categoryMapper :: toResponse).toList();

        CategoryResponse finalResponse = categoryMapper.toResponse(category);
        return new CategoryResponse(finalResponse.id(), finalResponse.name(), finalResponse.slug(), finalResponse.parentId(), actualChildren, finalResponse.createdAt(), finalResponse.updatedAt());
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "No category with id " + id));
        return categoryWithChildren(category);
    }

    @Cacheable(value = "CategoryTree")
    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream().map(categoryMapper::toResponse).toList();
    }

    @Transactional
    @CacheEvict(value = "CategoryTree", allEntries = true)
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "No category with id " + id));
        if (productRepository.existsByCategoryId(id)) {
            throw new ConflictException("CATEGORY_HAS_PRODUCTS", "Cannot delete category with existing products");
        }
        if (!categoryRepository.findByParentId(id).isEmpty()) {
            throw new ConflictException("CATEGORY_HAS_CHILDREN", "Cannot delete category with child categories");
        }
        categoryRepository.delete(category);
    }
    @Transactional
    @CacheEvict(value = "CategoryTree", allEntries = true)
    public CategoryResponse update(Long id, CreateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "No category with id " + id));

        category.setName(request.name());
        category.setSlug(request.slug());

        if (request.parentId() != null) {
            if (request.parentId().equals(id)) {
                throw new ConflictException("INVALID_PARENT", "A category cannot be its own parent");
            }
            Category parent = categoryRepository.findById(request.parentId())
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "No category with id " + request.parentId()));

            Category ancestor = parent;
            while (ancestor != null) {
                if (ancestor.getId().equals(id)) {
                    throw new ConflictException("INVALID_PARENT", "This change would create a category cycle");
                }
                ancestor = ancestor.getParent();
            }
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        return categoryWithChildren(category);
    }



}
