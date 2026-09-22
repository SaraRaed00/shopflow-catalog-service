package com.shopflow.catalog.service;

import com.shopflow.catalog.domain.exception.ConflictException;
import com.shopflow.catalog.domain.exception.NotFoundException;
import com.shopflow.catalog.domain.model.*;
import com.shopflow.catalog.repository.ProductRepository;
import com.shopflow.catalog.repository.StockItemRepository;
import com.shopflow.catalog.repository.WarehouseRepository;
import com.shopflow.catalog.web.dto.AdjustStockRequest;
import com.shopflow.catalog.web.dto.StockResponse;
import com.shopflow.catalog.web.dto.TransferStockRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private StockItemRepository stockItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private StockItem stockItem(int quantity, int reserved) {
        StockItem item = new StockItem();
        item.setQuantity(quantity);
        item.setReservedQty(reserved);
        item.setProduct(new Product());
        item.setWarehouse(new Warehouse());
        return item;
    }

    @Test
    void adjust_shouldIncreaseQuantity_whenPositiveChange() {
        StockItem item = stockItem(10, 2);
        AdjustStockRequest request = new AdjustStockRequest(1L, 1L, 5, "restock");
        when(stockItemRepository.findByProductIdAndWarehouseId(1L, 1L)).thenReturn(Optional.of(item));

        inventoryService.adjust(request);

        assertThat(item.getQuantity()).isEqualTo(15);
    }

    @Test
    void adjust_shouldThrowConflict_whenResultDropsBelowReserved() {
        StockItem item = stockItem(10, 8);
        AdjustStockRequest request = new AdjustStockRequest(1L, 1L, -5, "damage");
        when(stockItemRepository.findByProductIdAndWarehouseId(1L, 1L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> inventoryService.adjust(request))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    void transfer_shouldThrowConflict_whenInsufficientAvailable() {
        StockItem from = stockItem(10, 8); // only 2 available
        TransferStockRequest request = new TransferStockRequest(1L, 1L, 2L, 5);
        when(stockItemRepository.findByProductIdAndWarehouseId(1L, 1L)).thenReturn(Optional.of(from));

        assertThatThrownBy(() -> inventoryService.transfer(request))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Not enough");
    }

    @Test
    void transfer_shouldThrowNotFound_whenSourceStockMissing() {
        TransferStockRequest request = new TransferStockRequest(1L, 1L, 2L, 5);
        when(stockItemRepository.findByProductIdAndWarehouseId(1L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.transfer(request))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getStockForProduct_shouldReturnAvailableCalculatedCorrectly() {
        StockItem item = stockItem(50, 12);
        item.setWarehouse(new Warehouse());
        when(stockItemRepository.findByProductId(1L)).thenReturn(List.of(item));

        List<StockResponse> result = inventoryService.getStockForProduct(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).available()).isEqualTo(38);
    }
    @Test
    void transfer_shouldCreateDestinationStockItem_whenNoneExistsYet() {
        StockItem from = stockItem(20, 0);
        Product product = new Product();
        Warehouse toWarehouse = new Warehouse();
        TransferStockRequest request = new TransferStockRequest(1L, 1L, 2L, 5);

        when(stockItemRepository.findByProductIdAndWarehouseId(1L, 1L)).thenReturn(Optional.of(from));
        when(stockItemRepository.findByProductIdAndWarehouseId(1L, 2L)).thenReturn(Optional.empty());
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(toWarehouse));
        when(stockItemRepository.save(any(StockItem.class))).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.transfer(request);

        assertThat(from.getQuantity()).isEqualTo(15);
    }
    @Test
    void adjust_shouldCreateNewStockItem_whenNoneExistsYet() {
        Product product = new Product();
        Warehouse warehouse = new Warehouse();
        AdjustStockRequest request = new AdjustStockRequest(1L, 1L, 20, "initial stock");

        when(stockItemRepository.findByProductIdAndWarehouseId(1L, 1L)).thenReturn(Optional.empty());
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(stockItemRepository.save(any(StockItem.class))).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.adjust(request);

        verify(stockItemRepository).save(any(StockItem.class));
    }

}
