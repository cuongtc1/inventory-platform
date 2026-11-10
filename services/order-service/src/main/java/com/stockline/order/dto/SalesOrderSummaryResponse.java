package com.stockline.order.dto;

import com.stockline.order.constant.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One row of the order list: the mockup's Order, Customer, Date, Total and Status, without the lines.
 */
public record SalesOrderSummaryResponse(
		Long id,
		String orderNumber,
		Long customerId,
		String customerName,
		String warehouseCode,
		OrderStatus status,
		BigDecimal totalAmount,
		Instant createdAt,
		String createdBy
) {
}
