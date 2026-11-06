package com.stockline.inventory.controller;

import com.stockline.common.dto.PageResponse;
import com.stockline.inventory.dto.StockMovementFilter;
import com.stockline.inventory.dto.StockMovementResponse;
import com.stockline.inventory.service.StockMovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock-movements")
@Tag(name = "Stock movements")
public class StockMovementController {

	private final StockMovementService stockMovementService;

	public StockMovementController(StockMovementService stockMovementService) {
		this.stockMovementService = stockMovementService;
	}

	@GetMapping
	@Operation(summary = "List stock movements",
			description = "The history of stock, newest first. `search` matches part of the product name or SKU. "
					+ "`from` and `to` are Vietnam dates (YYYY-MM-DD), both included. All filters are optional")
	public PageResponse<StockMovementResponse> list(
			@ParameterObject StockMovementFilter filter,
			@ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
			Pageable pageable) {
		return stockMovementService.list(filter, pageable);
	}
}
