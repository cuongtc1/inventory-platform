package com.stockline.auth.dto;

import com.stockline.common.constant.UserType;
import com.stockline.common.util.CommonValidationUtils;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body of PUT /api/users/{id} (ADMIN). No password (each user changes their own) and no status (that is
 * POST .../deactivate and .../activate).
 */
public record UpdateUserRequest(
		@NotBlank @Email @Size(max = CommonValidationUtils.EMAIL_MAX_LENGTH) String email,
		@NotBlank @Size(max = CommonValidationUtils.NAME_MAX_LENGTH) String fullName,
		@NotNull UserType userType
) {
}
