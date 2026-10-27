package com.stockline.product.dto;

import java.time.Instant;

/**
 * A category as returned by the API. {@code parentId} and {@code parentName} are null for a top-level category.
 * {@code productCount} counts the products directly in this category, not in its subcategories.
 * {@code createdBy} / {@code updatedBy} are the user's login, or "system" (before login exists, or for jobs).
 */
public record CategoryResponse(
		Long id,
		String name,
		Long parentId,
		String parentName,
		long productCount,
		Instant createdAt,
		String createdBy,
		Instant updatedAt,
		String updatedBy
) {
}
