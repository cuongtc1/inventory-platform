package com.stockline.product.dto;

import java.time.Instant;

/**
 * A category as returned by the API. {@code parentId} and {@code parentName} are null for a top-level category.
 */
public record CategoryResponse(
        Long id,
        String name,
        Long parentId,
        String parentName,
        Instant createdAt,
        Instant updatedAt
) {
}
