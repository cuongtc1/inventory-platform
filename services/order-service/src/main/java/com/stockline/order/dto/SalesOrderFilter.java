package com.stockline.order.dto;

import com.stockline.order.constant.OrderStatus;

/**
 * Optional filters of GET /api/sales-orders, filled from query parameters with the same names:
 * {@code ?search=acme&customerId=3&status=PENDING}. A null field means "don't filter on this".
 */
public record SalesOrderFilter(
		String search,
		Long customerId,
		OrderStatus status
) {
}
