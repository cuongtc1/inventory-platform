package com.stockline.product.mapper;

import com.stockline.product.dto.ProductResponse;
import com.stockline.product.entity.Category;
import com.stockline.product.entity.Product;
import com.stockline.product.entity.Supplier;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

	/**
	 * Reads {@code category} and {@code supplier}, which are lazy-loaded, so call this inside a transaction.
	 */
	public ProductResponse toResponse(Product product) {
		Category category = product.getCategory();
		Supplier supplier = product.getSupplier();
		return new ProductResponse(
				product.getId(),
				product.getSku(),
				product.getName(),
				category.getId(),
				category.getName(),
				supplier == null ? null : supplier.getId(),
				supplier == null ? null : supplier.getName(),
				product.getCostPrice(),
				product.getSalePrice(),
				product.getReorderLevel(),
				product.getStatus(),
				product.getCreatedAt(),
				product.getCreatedBy(),
				product.getUpdatedAt(),
				product.getUpdatedBy());
	}
}
