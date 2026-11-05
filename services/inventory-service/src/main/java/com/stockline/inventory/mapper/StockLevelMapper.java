package com.stockline.inventory.mapper;

import com.stockline.inventory.dto.StockLevelResponse;
import com.stockline.inventory.entity.StockLevel;
import org.springframework.stereotype.Component;

@Component
public class StockLevelMapper {

	public StockLevelResponse toResponse(StockLevel level) {
		return new StockLevelResponse(
				level.getId(),
				level.getWarehouse().getId(),
				level.getWarehouse().getCode(),
				level.getWarehouse().getName(),
				level.getProductId(),
				level.getProductSku(),
				level.getProductName(),
				level.getOnHand(),
				level.getReserved(),
				level.getAvailable(),
				level.getReorderLevel(),
				level.isLowStock(),
				level.getCreatedAt(),
				level.getCreatedBy(),
				level.getUpdatedAt(),
				level.getUpdatedBy());
	}
}
