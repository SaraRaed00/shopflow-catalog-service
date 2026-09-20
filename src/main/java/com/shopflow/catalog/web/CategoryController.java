package com.shopflow.catalog.web;

import com.shopflow.catalog.service.CategoryService;
import com.shopflow.catalog.web.dto.CategoryResponse;
import com.shopflow.catalog.web.dto.CreateCategoryRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService){
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request){
        CategoryResponse response = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
        //return ResponseEntity.created(URI.create("/api/v1/categories/" + response.id())).body(response);
        //ResponseEntity.created(URI: to build a response with coe 201 and set the location header to the URI
    }
    @GetMapping("/{id}")
    public CategoryResponse findById(@PathVariable Long id){

        return categoryService.findById(id);
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CreateCategoryRequest request){
        return categoryService.update(id,request);
    }

    // Listing the category tree
    @GetMapping
    public List<CategoryResponse> findAll() {
        return categoryService.findAll();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }


}
