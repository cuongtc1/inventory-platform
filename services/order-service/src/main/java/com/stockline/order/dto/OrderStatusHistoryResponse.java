package com.stockline.order.dto;

import com.stockline.order.constant.OrderStatus;

import java.time.Instant;

/**
 * One row of an order's timeline: the mockup's "10:15 PENDING by staff1". {@code fromStatus} is null on the first
 * row. {@code createdBy} is who made the change, or "system" for a Kafka event (Phase 6).
 */
public record OrderStatusHistoryResponse(
		Long id,
		OrderStatus fromStatus,
		OrderStatus toStatus,
		String note,
		Instant createdAt,
		String createdBy
) {
}
