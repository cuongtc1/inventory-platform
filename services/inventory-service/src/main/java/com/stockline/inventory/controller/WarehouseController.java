package com.stockline.inventory.controller;

import com.stockline.common.dto.PageResponse;
import com.stockline.inventory.dto.WarehouseRequest;
import com.stockline.inventory.dto.WarehouseResponse;
import com.stockline.inventory.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/warehouses")
@Tag(name = "Warehouses")
public class WarehouseController {

	private final WarehouseService warehouseService;

	public WarehouseController(WarehouseService warehouseService) {
		this.warehouseService = warehouseService;
	}

	@GetMapping
	@Operation(summary = "List warehouses", description = "`search` matches part of the code or name, ignoring case")
	public PageResponse<WarehouseResponse> list(
			@RequestParam(required = false) String search,
			// size must be repeated here: @PageableDefault's own default is 10, not the global 20
			@ParameterObject @PageableDefault(size = 20, sort = "code") Pageable pageable) {
		return warehouseService.list(search, pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get one warehouse")
	public WarehouseResponse get(@PathVariable Long id) {
		return warehouseService.get(id);
	}

	@PostMapping
	@Operation(summary = "Create a warehouse")
	public ResponseEntity<WarehouseResponse> create(@Valid @RequestBody WarehouseRequest request) {
		WarehouseResponse created = warehouseService.create(request);
		return ResponseEntity.created(URI.create("/api/warehouses/" + created.id())).body(created);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update a warehouse")
	public WarehouseResponse update(@PathVariable Long id, @Valid @RequestBody WarehouseRequest request) {
		return warehouseService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete a warehouse", description = "Fails with 409 IN_USE if it has stock levels")
	public void delete(@PathVariable Long id) {
		warehouseService.delete(id);
	}
}
