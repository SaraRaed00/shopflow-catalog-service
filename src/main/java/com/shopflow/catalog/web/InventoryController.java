package com.shopflow.catalog.web;

import com.shopflow.catalog.service.InventoryService;
import com.shopflow.catalog.web.dto.AdjustStockRequest;
import com.shopflow.catalog.web.dto.TransferStockRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/adjust")
    public ResponseEntity<Void> adjust(@Valid @RequestBody AdjustStockRequest request) {
        inventoryService.adjust(request);
        return ResponseEntity.ok().build(); // 200
    }

    @PostMapping("/transfer")
    public ResponseEntity<Void> transfer(@Valid @RequestBody TransferStockRequest request) {
        inventoryService.transfer(request);
        return ResponseEntity.ok().build(); // 200
    }
}
