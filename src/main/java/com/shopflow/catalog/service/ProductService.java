package com.shopflow.catalog.service;

import com.shopflow.catalog.domain.exception.ConflictException;
import com.shopflow.catalog.domain.exception.NotFoundException;
import com.shopflow.catalog.domain.model.Category;
import com.shopflow.catalog.domain.model.Money;
import com.shopflow.catalog.domain.model.Product;
import com.shopflow.catalog.domain.model.ProductStatus;
import com.shopflow.catalog.mapper.ProductMapper;
import com.shopflow.catalog.repository.CategoryRepository;
import com.shopflow.catalog.repository.ProductRepository;
import com.shopflow.catalog.repository.ProductSpecification;
import com.shopflow.catalog.web.dto.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository, ProductMapper productMapper){
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productMapper = productMapper;
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request){

        // check that the sku is not redundant
        if(productRepository.existsBySku(request.sku()))
            throw new ConflictException("DUPLICATE_SKU", "THIS SKU ALREADY EXISTS: "+ request.sku());

        // check that the product has a valid category id
        Category category = categoryRepository.findById(request.categoryId())
            .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "NO CATEGORY WITH THIS ID: "+ request.categoryId()));

        // start generating a new product
        Product product = new Product();
        product.setSku(request.sku());
        product.setName(request.name());
        product.setCategory(category);
        product.setDescription(request.description());
        product.setPrice(new Money(request.priceAmount(),request.priceCurrency()));

        return productMapper.toResponse(productRepository.save(product));
    }



    @Cacheable(value = "products", key = "#id")
    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return productMapper.toResponse(findEntityById(id));
    }

    @Cacheable(value = "productBySku", key = "#sku")
    @Transactional(readOnly = true)
    public ProductResponse findBySku(String sku) {
          return productMapper.toResponse(findEntityBySku(sku));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(ProductSearchCriteria criteria, Pageable pageable) {
        // needed to convert to a list of ProductResponse(DTOs)
        Page<Product> page = productRepository.findAll(ProductSpecification.matching(criteria), pageable); // get content to pull only the List of Products
        List<ProductResponse> content = page.getContent().stream().map(productMapper :: toResponse).toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
            page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", key = "#id"),
        @CacheEvict(value = "productBySku", key = "#result.sku()")
    })
    public ProductResponse update(Long id, UpdateProductRequest request) {
        Product product = findEntityById(id);
        Category category = categoryRepository.findById(request.categoryId())
            .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "No category with id " + request.categoryId() ));
        product.setName(request.name());
        product.setDescription(request.description());
        product.setCategory(category);
        product.setPrice(new Money(request.priceAmount(), request.priceCurrency()));
        return productMapper.toResponse(product);
    }// try not to delete all sku cache

    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public ProductResponse changeStatus(Long id, ProductStatus target) {
        Product product = findEntityById(id);
        product.changeStatus(target);
        return productMapper.toResponse(product);
    }

    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public void discontinue(Long id) {

        changeStatus(id, ProductStatus.DISCONTINUED);
    }




    private Product findEntityById(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "No product with id " + id));
    }

    private Product findEntityBySku(String sku){
        return productRepository.findBySku(sku)
            .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "NO PRODUCT WITH THIS SKU: "+ sku) );
    }

    @Transactional(readOnly = true)
    public keysetPageResponse<ProductResponse> feed(ProductSearchCriteria criteria, Long afterId, int size){
        Specification<Product> spec = ProductSpecification.matching(criteria);
        if (afterId != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThan(root.get("id"), afterId));
        }
        List<Product> rows = productRepository.findBy(spec,
            q -> q.sortBy(Sort.by("id").ascending()).limit(size + 1).all());

        boolean hasNext = rows.size() > size;
        List<Product> pageRows = hasNext ? rows.subList(0, size) : rows;

        List<ProductResponse> content = pageRows.stream().map(productMapper::toResponse).toList();
        Long nextCursor = hasNext ? pageRows.get(pageRows.size() - 1).getId() : null;

        return new keysetPageResponse<>(content, nextCursor, hasNext);
    }

}
