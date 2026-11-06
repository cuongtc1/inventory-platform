package com.stockline.inventory.dto;

import com.stockline.inventory.constant.MovementType;

import java.time.Instant;

/**
 * One change of stock. {@code quantity} is signed: -2 took two away. {@code createdAt} and {@code createdBy} say when
 * and who; {@code productSku} and {@code productName} are as they were at that moment.
 */
public record StockMovementResponse(
		Long id,
		Long warehouseId,
		String warehouseCode,
		Long productId,
		String productSku,
		String productName,
		MovementType movementType,
		int quantity,
		String reference,
		Instant createdAt,
		String createdBy,
		Instant updatedAt,
		String updatedBy
) {
}
