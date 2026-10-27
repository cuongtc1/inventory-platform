package com.stockline.product.dto;

import java.time.Instant;

/**
 * A supplier as returned by the API. Contact fields are null when not set.
 * {@code createdBy} / {@code updatedBy} are the user's login, or "system" (before login exists, or for jobs).
 */
public record SupplierResponse(
		Long id,
		String name,
		String contactName,
		String email,
		String phone,
		Instant createdAt,
		String createdBy,
		Instant updatedAt,
		String updatedBy
) {
}
