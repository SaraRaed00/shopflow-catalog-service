package com.shopflow.catalog.web;

import com.shopflow.catalog.service.InventoryService;
import com.shopflow.catalog.web.dto.AdjustStockRequest;
import com.shopflow.catalog.web.dto.ApiError;
import com.shopflow.catalog.web.dto.TransferStockRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Inventory", description = "Stock adjustment and transfer between warehouses")
@RequestMapping("/api/v1/inventory")
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Operation(summary = "Adjust stock", description = "Add or remove stock for a product/warehouse, with a reason. Creates the stock record if it doesn't exist yet.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stock adjusted"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Product or warehouse not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Adjustment would drop quantity below reserved amount", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/adjust")
    public ResponseEntity<Void> adjust(@Valid @RequestBody AdjustStockRequest request) {
        inventoryService.adjust(request);
        return ResponseEntity.ok().build(); // 200
    }

    @Operation(summary = "Transfer stock between warehouses")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stock transferred"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Product, source, or destination warehouse not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Not enough available stock to transfer", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/transfer")
    public ResponseEntity<Void> transfer(@Valid @RequestBody TransferStockRequest request) {
        inventoryService.transfer(request);
        return ResponseEntity.ok().build(); // 200
    }
}
