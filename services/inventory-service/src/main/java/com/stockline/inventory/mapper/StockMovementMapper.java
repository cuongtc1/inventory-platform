package com.stockline.inventory.mapper;

import com.stockline.inventory.dto.StockMovementResponse;
import com.stockline.inventory.entity.StockMovement;
import org.springframework.stereotype.Component;

@Component
public class StockMovementMapper {

	public StockMovementResponse toResponse(StockMovement movement) {
		return new StockMovementResponse(
				movement.getId(),
				movement.getWarehouse().getId(),
				movement.getWarehouse().getCode(),
				movement.getProductId(),
				movement.getProductSku(),
				movement.getProductName(),
				movement.getMovementType(),
				movement.getQuantity(),
				movement.getReference(),
				movement.getCreatedAt(),
				movement.getCreatedBy(),
				movement.getUpdatedAt(),
				movement.getUpdatedBy());
	}
}
