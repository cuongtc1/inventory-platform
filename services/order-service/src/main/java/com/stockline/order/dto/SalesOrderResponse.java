package com.stockline.order.dto;

import com.stockline.order.constant.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * One sales order with its lines, for the detail page. Amounts are USD with 2 decimals. {@code createdBy} is who took
 * the order.
 */
public record SalesOrderResponse(
		Long id,
		String orderNumber,
		Long customerId,
		String customerName,
		Long warehouseId,
		String warehouseCode,
		OrderStatus status,
		BigDecimal totalAmount,
		List<Item> items,
		Instant createdAt,
		String createdBy,
		Instant updatedAt,
		String updatedBy
) {

	/** One line. SKU, name and price are as they were when the order was created. */
	public record Item(
			Long id,
			Long productId,
			String productSku,
			String productName,
			int quantity,
			BigDecimal unitPrice,
			BigDecimal lineTotal
	) {
	}
}
