package com.shopflow.catalog.web;

import com.shopflow.catalog.domain.model.ProductStatus;
import com.shopflow.catalog.service.InventoryService;
import com.shopflow.catalog.service.ProductService;
import com.shopflow.catalog.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService productService;
    private final InventoryService inventoryService;

    public ProductController(ProductService productService, InventoryService inventoryService) {
        this.productService = productService;
        this.inventoryService = inventoryService;
    }

    // wrap in response entity to control the status code and headers
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request){
        ProductResponse response = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
        //ResponseEntity.created(URI: to build a response with code 201 and set the location header to the URI
    }

    @GetMapping("/{id}")
    public ProductResponse findById(@PathVariable Long id){
        return productService.findById(id);
    }

    @GetMapping("/sku/{sku}")
    public ProductResponse findBySku(@PathVariable String sku){
        return productService.findBySku(sku);
    }

    @GetMapping
    public PageResponse<ProductResponse> search(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false)ProductStatus status,
        @RequestParam(required = false)BigDecimal minPrice,
        @RequestParam(required = false)BigDecimal maxPrice,
        @PageableDefault(size = 20) Pageable pageable)
        {
        ProductSearchCriteria criteria = new ProductSearchCriteria(q, categoryId, status, minPrice, maxPrice);
        return productService.search(criteria, pageable);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request){
        return productService.update(id,request);
    }

    @PatchMapping("{id}/status")
    public ProductResponse changeStatus(@PathVariable Long id, @Valid @RequestBody UpdateProductStatusRequest request){
        return productService.changeStatus(id,request.status());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        productService.discontinue(id);
        return ResponseEntity.noContent().build(); // to return 204
    }

    @GetMapping("/{id}/stock")
    public List<StockResponse> getStock(@PathVariable Long id) {
        return inventoryService.getStockForProduct(id);
    }






}
