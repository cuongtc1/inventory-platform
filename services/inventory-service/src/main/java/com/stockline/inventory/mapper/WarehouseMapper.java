package com.stockline.inventory.mapper;

import com.stockline.inventory.dto.WarehouseResponse;
import com.stockline.inventory.entity.Warehouse;
import org.springframework.stereotype.Component;

@Component
public class WarehouseMapper {

	public WarehouseResponse toResponse(Warehouse warehouse, long skuCount) {
		return new WarehouseResponse(
				warehouse.getId(),
				warehouse.getCode(),
				warehouse.getName(),
				warehouse.getAddress(),
				skuCount,
				warehouse.getCreatedAt(),
				warehouse.getCreatedBy(),
				warehouse.getUpdatedAt(),
				warehouse.getUpdatedBy());
	}
}
