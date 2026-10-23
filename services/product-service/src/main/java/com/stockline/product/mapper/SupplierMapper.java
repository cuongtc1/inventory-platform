package com.stockline.product.mapper;

import com.stockline.product.dto.SupplierResponse;
import com.stockline.product.entity.Supplier;
import org.springframework.stereotype.Component;

@Component
public class SupplierMapper {

	public SupplierResponse toResponse(Supplier supplier) {
		return new SupplierResponse(
				supplier.getId(),
				supplier.getName(),
				supplier.getContactName(),
				supplier.getEmail(),
				supplier.getPhone(),
				supplier.getCreatedAt(),
				supplier.getUpdatedAt());
	}
}
