package com.stockline.auth.dto;

import com.stockline.auth.constant.UserStatus;
import com.stockline.auth.constant.UserType;

import java.time.Instant;

/**
 * A user as returned by the API. Never contains the password hash.
 * {@code createdBy} / {@code updatedBy} are the email of the user who made the change, or "system".
 */
public record UserResponse(
		Long id,
		String email,
		String fullName,
		UserType userType,
		UserStatus status,
		Instant createdAt,
		String createdBy,
		Instant updatedAt,
		String updatedBy
) {
}
