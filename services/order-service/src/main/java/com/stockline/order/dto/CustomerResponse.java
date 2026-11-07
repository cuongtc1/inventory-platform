package com.stockline.order.dto;

import java.time.Instant;

/**
 * A customer as returned by the API. {@code email} and {@code phone} are null when not set.
 * {@code orderCount} is the number of sales orders of this customer.
 * {@code createdBy} / {@code updatedBy} are the user's login, or "system".
 */
public record CustomerResponse(
		Long id,
		String name,
		String email,
		String phone,
		long orderCount,
		Instant createdAt,
		String createdBy,
		Instant updatedAt,
		String updatedBy
) {
}
