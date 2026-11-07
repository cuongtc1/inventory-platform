package com.stockline.order.controller;

import com.stockline.common.dto.PageResponse;
import com.stockline.order.dto.CustomerRequest;
import com.stockline.order.dto.CustomerResponse;
import com.stockline.order.service.CustomerService;
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
@RequestMapping("/api/customers")
@Tag(name = "Customers")
public class CustomerController {

	private final CustomerService customerService;

	public CustomerController(CustomerService customerService) {
		this.customerService = customerService;
	}

	@GetMapping
	@Operation(summary = "List customers", description = "`search` matches part of the name or email, ignoring case")
	public PageResponse<CustomerResponse> list(
			@RequestParam(required = false) String search,
			// size must be repeated here: @PageableDefault's own default is 10, not the global 20
			@ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable) {
		return customerService.list(search, pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get one customer")
	public CustomerResponse get(@PathVariable Long id) {
		return customerService.get(id);
	}

	@PostMapping
	@Operation(summary = "Create a customer")
	public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
		CustomerResponse created = customerService.create(request);
		return ResponseEntity.created(URI.create("/api/customers/" + created.id())).body(created);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update a customer")
	public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
		return customerService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete a customer", description = "MANAGER and up. Fails with 409 IN_USE if it has orders")
	public void delete(@PathVariable Long id) {
		customerService.delete(id);
	}
}
