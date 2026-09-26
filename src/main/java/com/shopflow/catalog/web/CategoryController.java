package com.shopflow.catalog.web;

import com.shopflow.catalog.service.CategoryService;
import com.shopflow.catalog.web.dto.ApiError;
import com.shopflow.catalog.web.dto.CategoryResponse;
import com.shopflow.catalog.web.dto.CreateCategoryRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
@Tag(name = "Categories", description = "Category tree CRUD")
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService){

        this.categoryService = categoryService;
    }

    @Operation(summary = "Create a category")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Category created"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Parent category not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request){
        CategoryResponse response = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
        //return ResponseEntity.created(URI.create("/api/v1/categories/" + response.id())).body(response);
        //ResponseEntity.created(URI: to build a response with coe 201 and set the location header to the URI
    }

    @Operation(summary = "Get a category by id", description = "Includes direct children (one level deep).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Category found"),
        @ApiResponse(responseCode = "404", description = "Category not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public CategoryResponse findById(@PathVariable Long id){

        return categoryService.findById(id);
    }


    @Operation(summary = "Update a category")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Category updated"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Category or parent not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CreateCategoryRequest request){
        return categoryService.update(id,request);
    }

    // Listing the category tree
    @Operation(summary = "List all categories", description = "Flat list; children are not populated on this endpoint.")
    @ApiResponse(responseCode = "200", description = "All categories")
    @GetMapping
    public List<CategoryResponse> findAll() {

        return categoryService.findAll();
    }

    @Operation(summary = "Delete a category", description = "Rejected if the category has products or child categories.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Category deleted"),
        @ApiResponse(responseCode = "404", description = "Category not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Category has products or child categories", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }


}
