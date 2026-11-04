package com.stockline.auth.dto;

import com.stockline.auth.constant.PasswordPolicy;
import com.stockline.common.constant.UserType;
import com.stockline.common.util.CommonValidationUtils;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/users (ADMIN). The ADMIN chooses the first password and tells it to the user, who can change
 * it with PUT /api/auth/password. A new user is always ACTIVE.
 */
public record CreateUserRequest(
		@NotBlank @Email @Size(max = CommonValidationUtils.EMAIL_MAX_LENGTH) String email,
		@NotBlank @Size(max = CommonValidationUtils.NAME_MAX_LENGTH) String fullName,
		@NotNull UserType userType,
		@NotBlank @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH) String password
) {
}
