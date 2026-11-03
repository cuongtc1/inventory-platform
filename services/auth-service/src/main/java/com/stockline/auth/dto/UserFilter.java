package com.stockline.auth.dto;

import com.stockline.auth.constant.UserStatus;
import com.stockline.auth.constant.UserType;

/**
 * Optional filters of GET /api/users, filled from query parameters with the same names:
 * {@code ?search=linh&userType=MANAGER&status=ACTIVE}. A null field means "don't filter on this".
 */
public record UserFilter(
		String search,
		UserType userType,
		UserStatus status
) {
}
