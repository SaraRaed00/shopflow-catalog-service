package com.shopflow.catalog.web;

import com.shopflow.catalog.domain.exception.InvalidRequestException;
import com.shopflow.catalog.domain.model.ProductStatus;
import com.shopflow.catalog.service.InventoryService;
import com.shopflow.catalog.service.ProductService;
import com.shopflow.catalog.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Pageable;

@Tag(name = "Products", description = "Product catalog CRUD, search and stock lookups")
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService productService;
    private final InventoryService inventoryService;

    public ProductController(ProductService productService, InventoryService inventoryService) {
        this.productService = productService;
        this.inventoryService = inventoryService;
    }

    @Operation(summary = "Create a new product", description = "Create a product in status: DRAFT with Category available.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Product Created"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Category not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "SKU already exists", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/create")
    public ResponseEntity<ProductResponse> handleCreate(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse response = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @Operation(summary = "Search products", description = "Filter/sort/page products. Empty body returns all products.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Matching products, paged"),
        @ApiResponse(responseCode = "400", description = "Invalid sort field", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/search")
    public PageResponse<ProductResponse> handleSearch(@RequestBody(required = false) ProductSearchCriteria criteria, @PageableDefault(size = 20) Pageable pageable){

        for (Sort.Order order: pageable.getSort()){
            if(!ALLOWED_SORT_FIELDS.contains(order.getProperty() // field requested by user
            ))
                throw new InvalidRequestException("INVALID_SORT_FIELD", "CANNOT SORT BY THIS FIELD: "+ order.getProperty());
        }

        ProductSearchCriteria finalCriteria;
        if(criteria == null)
            finalCriteria = new ProductSearchCriteria(null, null, null, null, null);
        else
            finalCriteria = criteria;

        // max cap size is 100
        int safeSize = Math.min(pageable.getPageSize(), 100);

        Pageable safePageable  = PageRequest.of(pageable.getPageNumber(),safeSize, pageable.getSort());


        return productService.search(finalCriteria, safePageable);
    }


    /*
    // wrap in response entity to control the status code and headers
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request){
        ProductResponse response = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
        //ResponseEntity.created(URI: to build a response with code 201 and set the location header to the URI
    }
    */

    @Operation(summary = "Get a product by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product found"),
        @ApiResponse(responseCode = "404", description = "No product with this id", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public ProductResponse findById(@PathVariable Long id){
        return productService.findById(id);
    }

    @Operation(summary = "Get a product by SKU")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product found"),
        @ApiResponse(responseCode = "404", description = "No product with this SKU", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/sku/{sku}")
    public ProductResponse findBySku(@PathVariable String sku){
        return productService.findBySku(sku);
    }

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "sku", "name" , "priceAmount", "status", "createdAt", "updatedAt");

    /*
    @GetMapping
    public PageResponse<ProductResponse> search(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false)ProductStatus status,
        @RequestParam(required = false)BigDecimal minPrice,
        @RequestParam(required = false)BigDecimal maxPrice,
        @PageableDefault(size = 20) Pageable pageable)
        {
            for (Sort.Order order: pageable.getSort()){
                if(!ALLOWED_SORT_FIELDS.contains(order.getProperty() // field requested by user
                ))
                    throw new InvalidRequestException("INVALID_SORT_FIELD", "CANNOT SORT BY THIS FIELD: "+ order.getProperty());
        }
            // max cap size is 100
        int safeSize = Math.min(pageable.getPageSize(), 100);
        Pageable safePageable  = PageRequest.of(pageable.getPageNumber(),safeSize, pageable.getSort());
        ProductSearchCriteria criteria = new ProductSearchCriteria(q, categoryId, status, minPrice, maxPrice);
        return productService.search(criteria, safePageable);
    }

     */

    @Operation(summary = "Update a product", description = "Full update of name, description, category, and price.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product updated"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Product or category not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request){
        return productService.update(id,request);
    }

    @Operation(summary = "Change product lifecycle status", description = "Legal transitions: DRAFT -> ACTIVE -> DISCONTINUED only.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status changed"),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Illegal status transition", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PatchMapping("{id}/status")
    public ProductResponse changeStatus(@PathVariable Long id, @Valid @RequestBody UpdateProductStatusRequest request){
        return productService.changeStatus(id,request.status());
    }

    @Operation(summary = "Discontinue a product", description = "Soft delete: sets status to DISCONTINUED, row is never removed.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Product discontinued"),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Illegal status transition", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        productService.discontinue(id);
        return ResponseEntity.noContent().build(); // to return 204
    }

    @Operation(summary = "Get stock levels for a product", description = "Stock across all warehouses, with available quantity computed.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stock levels returned")
    })
    @GetMapping("/{id}/stock")
    public List<StockResponse> getStock(@PathVariable Long id) {

        return inventoryService.getStockForProduct(id);
    }
}
