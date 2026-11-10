package com.stockline.order.controller;

import com.stockline.common.dto.PageResponse;
import com.stockline.order.constant.OrderStatus;
import com.stockline.order.dto.CreateSalesOrderRequest;
import com.stockline.order.dto.OrderStatusHistoryResponse;
import com.stockline.order.dto.SalesOrderFilter;
import com.stockline.order.dto.SalesOrderResponse;
import com.stockline.order.dto.SalesOrderSummaryResponse;
import com.stockline.order.service.OrderStatusService;
import com.stockline.order.service.SalesOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/sales-orders")
@Tag(name = "Sales orders")
public class SalesOrderController {

	private final SalesOrderService salesOrderService;
	private final OrderStatusService orderStatusService;

	public SalesOrderController(SalesOrderService salesOrderService, OrderStatusService orderStatusService) {
		this.salesOrderService = salesOrderService;
		this.orderStatusService = orderStatusService;
	}

	@GetMapping("/{id}/history")
	@Operation(summary = "Status history of an order", description = "Oldest first; the first row has no fromStatus")
	public List<OrderStatusHistoryResponse> history(@PathVariable Long id) {
		return orderStatusService.history(id);
	}

	// The actions: POST with a verb, no body (docs/conventions.md section 2). The state machine in OrderStatus
	// decides whether the change is allowed; 409 INVALID_STATUS_TRANSITION if not. Their stock effects come in T6.6.

	@PostMapping("/{id}/pay")
	@Operation(summary = "Mark as paid", description = "RESERVED → PAID. A fake payment: no payment provider")
	public SalesOrderResponse pay(@PathVariable Long id) {
		return orderStatusService.changeStatus(id, OrderStatus.PAID, null);
	}

	@PostMapping("/{id}/ship")
	@Operation(summary = "Mark as shipped", description = "PAID → SHIPPED")
	public SalesOrderResponse ship(@PathVariable Long id) {
		return orderStatusService.changeStatus(id, OrderStatus.SHIPPED, null);
	}

	@PostMapping("/{id}/complete")
	@Operation(summary = "Mark as completed", description = "SHIPPED → COMPLETED, once the customer has the goods")
	public SalesOrderResponse complete(@PathVariable Long id) {
		return orderStatusService.changeStatus(id, OrderStatus.COMPLETED, null);
	}

	@PostMapping("/{id}/cancel")
	@Operation(summary = "Cancel an order", description = "From PENDING, RESERVED or PAID: only before shipping. MANAGER and up")
	public SalesOrderResponse cancel(@PathVariable Long id) {
		return orderStatusService.changeStatus(id, OrderStatus.CANCELLED, null);
	}

	@GetMapping
	@Operation(summary = "List sales orders",
			description = "Newest first. `search` matches part of the order number or the customer's name. "
					+ "All filters are optional")
	public PageResponse<SalesOrderSummaryResponse> list(
			@ParameterObject SalesOrderFilter filter,
			@ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
			Pageable pageable) {
		return salesOrderService.list(filter, pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get one sales order with its lines")
	public SalesOrderResponse get(@PathVariable Long id) {
		return salesOrderService.get(id);
	}

	@PostMapping
	@Operation(summary = "Create a sales order",
			description = "Created as PENDING. Name, SKU and price of each product come from product-service")
	public ResponseEntity<SalesOrderResponse> create(@Valid @RequestBody CreateSalesOrderRequest request) {
		SalesOrderResponse created = salesOrderService.create(request);
		return ResponseEntity.created(URI.create("/api/sales-orders/" + created.id())).body(created);
	}
}
