package com.stockline.product.controller;

import com.stockline.common.dto.PageResponse;
import com.stockline.product.dto.ProductFilter;
import com.stockline.product.dto.ProductRequest;
import com.stockline.product.dto.ProductResponse;
import com.stockline.product.service.ProductService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products")
public class ProductController {

	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@GetMapping
	@Operation(summary = "List products",
			description = "`search` matches part of the name or SKU, ignoring case. All filters are optional and "
					+ "combined with AND. Sort by a field (`sku`, `name`, `salePrice`, `createdAt`, ...) "
					+ "or by `category.name` / `supplier.name`")
	public PageResponse<ProductResponse> list(
			// No annotation needed: Spring fills the record from query parameters with the same names
			@ParameterObject ProductFilter filter,
			// size must be repeated here: @PageableDefault's own default is 10, not the global 20
			@ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable) {
		return productService.list(filter, pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get one product")
	public ProductResponse get(@PathVariable Long id) {
		return productService.get(id);
	}

	@PostMapping
	@Operation(summary = "Create a product")
	public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
		ProductResponse created = productService.create(request);
		return ResponseEntity.created(URI.create("/api/products/" + created.id())).body(created);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update a product")
	public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
		return productService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete a product")
	public void delete(@PathVariable Long id) {
		productService.delete(id);
	}
}
