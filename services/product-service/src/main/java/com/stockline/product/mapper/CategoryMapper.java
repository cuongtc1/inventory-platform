package com.stockline.product.mapper;

import com.stockline.product.dto.CategoryResponse;
import com.stockline.product.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

	/**
	 * Reads {@code parent}, which is lazy-loaded, so call this inside a transaction (in the service).
	 * The product count comes from a separate query in the service (products are another table).
	 */
	public CategoryResponse toResponse(Category category, long productCount) {
		Category parent = category.getParent();
		return new CategoryResponse(
				category.getId(),
				category.getName(),
				parent == null ? null : parent.getId(),
				parent == null ? null : parent.getName(),
				productCount,
				category.getCreatedAt(),
				category.getCreatedBy(),
				category.getUpdatedAt(),
				category.getUpdatedBy());
	}
}
