package com.stockline.inventory.controller;

import com.stockline.common.dto.PageResponse;
import com.stockline.inventory.dto.AdjustStockRequest;
import com.stockline.inventory.dto.StockLevelFilter;
import com.stockline.inventory.dto.StockLevelResponse;
import com.stockline.inventory.service.StockLevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock-levels")
@Tag(name = "Stock levels")
public class StockLevelController {

	private final StockLevelService stockLevelService;

	public StockLevelController(StockLevelService stockLevelService) {
		this.stockLevelService = stockLevelService;
	}

	@GetMapping
	@Operation(summary = "List stock levels",
			description = "`search` matches part of the product name or SKU. `lowStock=true` keeps rows whose available "
					+ "stock is at or below the reorder level. Sort by `productName`, `productSku`, `onHand`, "
					+ "`reserved`, `warehouse.code`, ...")
	public PageResponse<StockLevelResponse> list(
			@ParameterObject StockLevelFilter filter,
			@ParameterObject @PageableDefault(size = 20, sort = "productName") Pageable pageable) {
		return stockLevelService.list(filter, pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get one stock level")
	public StockLevelResponse get(@PathVariable Long id) {
		return stockLevelService.get(id);
	}

	@PostMapping("/adjust")
	@Operation(summary = "Adjust stock (MANAGER)",
			description = "`quantity` is the change, e.g. 50 or -2. Writes an ADJUST movement and refreshes the copied "
					+ "product data. 409 INSUFFICIENT_STOCK if it would take more than is available")
	public StockLevelResponse adjust(@Valid @RequestBody AdjustStockRequest request) {
		return stockLevelService.adjust(request);
	}
}
