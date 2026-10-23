package com.stockline.product.controller;

import com.stockline.product.dto.CategoryRequest;
import com.stockline.product.dto.CategoryResponse;
import com.stockline.product.dto.PageResponse;
import com.stockline.product.service.CategoryService;
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
@RequestMapping("/api/categories")
@Tag(name = "Categories")
public class CategoryController {

	private final CategoryService categoryService;

	public CategoryController(CategoryService categoryService) {
		this.categoryService = categoryService;
	}

	@GetMapping
	@Operation(summary = "List categories", description = "`search` matches part of the name, ignoring case")
	public PageResponse<CategoryResponse> list(
			@RequestParam(required = false) String search,
			// Spring fills Pageable from ?page=&size=&sort=; @ParameterObject shows them as separate fields in Swagger.
			// size must be repeated here: @PageableDefault's own default is 10, not the global 20
			@ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable) {
		return categoryService.list(search, pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get one category")
	public CategoryResponse get(@PathVariable Long id) {
		return categoryService.get(id);
	}

	@PostMapping
	@Operation(summary = "Create a category")
	public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
		CategoryResponse created = categoryService.create(request);
		return ResponseEntity.created(URI.create("/api/categories/" + created.id())).body(created);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update a category")
	public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
		return categoryService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete a category", description = "Fails with 409 IN_USE if it has subcategories or products")
	public void delete(@PathVariable Long id) {
		categoryService.delete(id);
	}
}
