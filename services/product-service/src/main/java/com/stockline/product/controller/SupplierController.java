package com.stockline.product.controller;

import com.stockline.common.dto.PageResponse;
import com.stockline.product.dto.SupplierRequest;
import com.stockline.product.dto.SupplierResponse;
import com.stockline.product.service.SupplierService;
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
@RequestMapping("/api/suppliers")
@Tag(name = "Suppliers")
public class SupplierController {

	private final SupplierService supplierService;

	public SupplierController(SupplierService supplierService) {
		this.supplierService = supplierService;
	}

	@GetMapping
	@Operation(summary = "List suppliers", description = "`search` matches part of the name, ignoring case")
	public PageResponse<SupplierResponse> list(
			@RequestParam(required = false) String search,
			// size must be repeated here: @PageableDefault's own default is 10, not the global 20
			@ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable) {
		return supplierService.list(search, pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get one supplier")
	public SupplierResponse get(@PathVariable Long id) {
		return supplierService.get(id);
	}

	@PostMapping
	@Operation(summary = "Create a supplier")
	public ResponseEntity<SupplierResponse> create(@Valid @RequestBody SupplierRequest request) {
		SupplierResponse created = supplierService.create(request);
		return ResponseEntity.created(URI.create("/api/suppliers/" + created.id())).body(created);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update a supplier")
	public SupplierResponse update(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
		return supplierService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete a supplier", description = "Fails with 409 IN_USE if it has products")
	public void delete(@PathVariable Long id) {
		supplierService.delete(id);
	}
}
