package com.stockline.inventory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/stock-levels/adjust. {@code quantity} is the change, not the new total: +50 or -2.
 * {@code reference} says why, e.g. "Count"; optional.
 */
public record AdjustStockRequest(
		@NotNull @Positive Long productId,
		@NotNull @Positive Long warehouseId,
		@NotNull @Min(-MAX_QUANTITY) @Max(MAX_QUANTITY) Integer quantity,
		@Size(max = 100) String reference
) {

	/** Keeps a typo from reaching the INT limits of the column. */
	public static final int MAX_QUANTITY = 1_000_000;
}
