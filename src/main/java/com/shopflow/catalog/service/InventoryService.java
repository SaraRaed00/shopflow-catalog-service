package com.shopflow.catalog.service;
import com.shopflow.catalog.domain.exception.ConflictException;
import com.shopflow.catalog.domain.exception.NotFoundException;
import com.shopflow.catalog.domain.model.Product;
import com.shopflow.catalog.domain.model.StockItem;
import com.shopflow.catalog.domain.model.Warehouse;
import com.shopflow.catalog.repository.ProductRepository;
import com.shopflow.catalog.repository.StockItemRepository;
import com.shopflow.catalog.repository.WarehouseRepository;
import com.shopflow.catalog.web.dto.AdjustStockRequest;
import com.shopflow.catalog.web.dto.StockResponse;
import com.shopflow.catalog.web.dto.TransferStockRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service

public class InventoryService {
    private final StockItemRepository stockItemRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;

    public InventoryService(StockItemRepository stockItemRepository, ProductRepository productRepository, WarehouseRepository warehouseRepository){
        this.stockItemRepository = stockItemRepository;
        this.productRepository= productRepository;
        this.warehouseRepository = warehouseRepository;
    }

    @Transactional
    public void adjust(AdjustStockRequest request) {
        StockItem stock = stockItemRepository.findByProductIdAndWarehouseId(request.productId(), request.warehouseId())
            .orElseGet(() -> createStockItem(request.productId(), request.warehouseId())); // create a new stock
        int newQty = stock.getQuantity() + request.quantityChange();
        if (newQty < stock.getReservedQty()) {
            throw new ConflictException("INSUFFICIENT_STOCK-CANNOT_PROCEED", "This Adjustment will drop quantity below reserved amount");
        }
        stock.setQuantity(newQty);
    }

    @Transactional
    public void transfer(TransferStockRequest requset) {

        StockItem from = stockItemRepository.findByProductIdAndWarehouseId(requset.productId(), requset.fromWarehouseId())
            .orElseThrow(() -> new NotFoundException("STOCK_NOT_FOUND", "No stock at source warehouse"));

        // check if we can transfer or the quantity is insufficient
        int available = from.getQuantity() - from.getReservedQty();
        if (available < requset.quantity()) {
            throw new ConflictException("INSUFFICIENT_STOCK", "Not enough available stock to transfer");
        }

        StockItem to = stockItemRepository.findByProductIdAndWarehouseId(requset.productId(), requset.toWarehouseId())
            .orElseGet(() -> createStockItem(requset.productId(), requset.toWarehouseId()));
        from.setQuantity(from.getQuantity() - requset.quantity());
        to.setQuantity(to.getQuantity() + requset.quantity());
    }

    @Transactional(readOnly = true)
    public List<StockResponse> getStockForProduct(Long productId) {
        return stockItemRepository.findByProductId(productId).stream()
            .map(item -> new StockResponse(item.getWarehouse().getId(), item.getWarehouse().getCode(),
                item.getQuantity(), item.getReservedQty(), item.getQuantity() - item.getReservedQty()))
            .toList();
    }

    private StockItem createStockItem(Long productId, Long warehouseId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "No product with id " + productId));
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
            .orElseThrow(() -> new NotFoundException("WAREHOUSE_NOT_FOUND", "No warehouse with id " + warehouseId));
        StockItem item = new StockItem();
        item.setProduct(product);
        item.setWarehouse(warehouse);
        item.setQuantity(0);
        item.setReservedQty(0);
        return stockItemRepository.save(item);
    }
}
