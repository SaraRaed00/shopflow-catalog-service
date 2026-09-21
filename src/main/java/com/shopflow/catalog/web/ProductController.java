package com.shopflow.catalog.web;

import com.shopflow.catalog.domain.exception.InvalidRequestException;
import com.shopflow.catalog.domain.model.ProductStatus;
import com.shopflow.catalog.service.InventoryService;
import com.shopflow.catalog.service.ProductService;
import com.shopflow.catalog.web.dto.*;
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

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService productService;
    private final InventoryService inventoryService;

    public ProductController(ProductService productService, InventoryService inventoryService) {
        this.productService = productService;
        this.inventoryService = inventoryService;
    }


    @PostMapping("/create")
    public ResponseEntity<ProductResponse> handleCreate(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse response = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

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


    @GetMapping("/{id}")
    public ProductResponse findById(@PathVariable Long id){
        return productService.findById(id);
    }

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
