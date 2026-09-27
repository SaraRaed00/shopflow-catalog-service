package com.shopflow.catalog.support;

import com.shopflow.catalog.domain.model.*;

import java.math.BigDecimal;
import java.util.UUID;

public final class ProductFixtures {

    private ProductFixtures() {}

    public static CategoryBuilder aCategory() {
        return new CategoryBuilder();
    }

    public static ProductBuilder aProduct() {
        return new ProductBuilder();
    }

    public static WarehouseBuilder aWarehouse() {
        return new WarehouseBuilder();
    }

    public static StockItemBuilder aStockItem() {
        return new StockItemBuilder();
    }

    public static class CategoryBuilder {
        private String name = "Test Category";
        private String slug = "test-category-" + UUID.randomUUID();
        private Category parent = null;

        public CategoryBuilder withName(String name) { this.name = name; return this; }
        public CategoryBuilder withSlug(String slug) { this.slug = slug; return this; }
        public CategoryBuilder withParent(Category parent) { this.parent = parent; return this; }

        public Category build() {
            Category category = new Category();
            category.setName(name);
            category.setSlug(slug);
            category.setParent(parent);
            return category;
        }
    }

    public static class ProductBuilder {
        private String sku = "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        private String name = "Test Product";
        private String description = "A product created for testing";
        private Category category;
        private BigDecimal priceAmount = new BigDecimal("9.999");
        private String priceCurrency = "KWD";
        private ProductStatus status = ProductStatus.DRAFT;

        public ProductBuilder withSku(String sku) { this.sku = sku; return this; }
        public ProductBuilder withName(String name) { this.name = name; return this; }
        public ProductBuilder withCategory(Category category) { this.category = category; return this; }
        public ProductBuilder withPrice(BigDecimal amount, String currency) {
            this.priceAmount = amount; this.priceCurrency = currency; return this;
        }
        public ProductBuilder withStatus(ProductStatus status) { this.status = status; return this; }

        public Product build() {
            Product product = new Product();
            product.setSku(sku);
            product.setName(name);
            product.setDescription(description);
            product.setCategory(category);
            product.setPrice(new Money(priceAmount, priceCurrency));
            product.setStatus(status);
            return product;
        }
    }

    public static class WarehouseBuilder {
        private String code = "WH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        private String name = "Test Warehouse";
        private String country = "KW";

        public WarehouseBuilder withCode(String code) { this.code = code; return this; }
        public WarehouseBuilder withName(String name) { this.name = name; return this; }
        public WarehouseBuilder withCountry(String country) { this.country = country; return this; }

        public Warehouse build() {
            Warehouse warehouse = new Warehouse();
            warehouse.setCode(code);
            warehouse.setName(name);
            warehouse.setCountry(country);
            return warehouse;
        }
    }

    public static class StockItemBuilder {
        private Product product;
        private Warehouse warehouse;
        private int quantity = 10;
        private int reservedQty = 0;

        public StockItemBuilder withProduct(Product product) { this.product = product; return this; }
        public StockItemBuilder withWarehouse(Warehouse warehouse) { this.warehouse = warehouse; return this; }
        public StockItemBuilder withQuantity(int quantity) { this.quantity = quantity; return this; }
        public StockItemBuilder withReservedQty(int reservedQty) { this.reservedQty = reservedQty; return this; }

        public StockItem build() {
            StockItem item = new StockItem();
            item.setProduct(product);
            item.setWarehouse(warehouse);
            item.setQuantity(quantity);
            item.setReservedQty(reservedQty);
            return item;
        }
    }
}
