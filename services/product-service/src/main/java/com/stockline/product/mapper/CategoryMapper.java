package com.stockline.product.mapper;

import com.stockline.product.dto.CategoryResponse;
import com.stockline.product.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

	/**
	 * Reads {@code parent}, which is lazy-loaded, so call this inside a transaction (in the service).
	 */
	public CategoryResponse toResponse(Category category) {
		Category parent = category.getParent();
		return new CategoryResponse(
				category.getId(),
				category.getName(),
				parent == null ? null : parent.getId(),
				parent == null ? null : parent.getName(),
				category.getCreatedAt(),
				category.getUpdatedAt());
	}
}
