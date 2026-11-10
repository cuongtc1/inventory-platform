package com.stockline.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Body of POST /api/sales-orders. Only ids and quantities: SKU, name and price come from product-service, the
 * warehouse code from inventory-service, so a client can't send its own price.
 *
 * <p>{@code List<@Valid @NotNull Item>}: annotations on the type inside the list (Bean Validation 2.0 type-use
 * annotations) check each line, and an error names the line: {@code items[1].quantity}.
 */
public record CreateSalesOrderRequest(
		@NotNull @Positive Long customerId,
		@NotNull @Positive Long warehouseId,
		@NotNull @Size(min = 1, max = MAX_ITEMS) List<@Valid @NotNull Item> items
) {

	/** Lines per order. More would mean a call to product-service for each, in one request. */
	public static final int MAX_ITEMS = 50;

	/** Keeps a typo from reaching the INT limits of the column. */
	public static final int MAX_QUANTITY = 1_000_000;

	/** One line: which product, how many. */
	public record Item(
			@NotNull @Positive Long productId,
			@NotNull @Positive @Max(MAX_QUANTITY) Integer quantity
	) {
	}
}
