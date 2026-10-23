package com.stockline.product.dto;

import java.time.Instant;

/**
 * A supplier as returned by the API. Contact fields are null when not set.
 */
public record SupplierResponse(
		Long id,
		String name,
		String contactName,
		String email,
		String phone,
		Instant createdAt,
		Instant updatedAt
) {
}
