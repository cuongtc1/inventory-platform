package com.stockline.inventory.dto;

import com.stockline.inventory.constant.MovementType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Optional filters of GET /api/stock-movements, filled from query parameters with the same names:
 * {@code ?search=mouse&warehouseId=1&productId=12&movementType=ADJUST&from=2026-10-01&to=2026-10-31}.
 * A null field means "don't filter on this". {@code from} and {@code to} are Vietnam dates, both included.
 */
public record StockMovementFilter(
		String search,
		Long warehouseId,
		Long productId,
		MovementType movementType,
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
) {
}
