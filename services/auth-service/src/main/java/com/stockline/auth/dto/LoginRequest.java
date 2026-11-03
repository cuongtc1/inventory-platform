package com.stockline.auth.dto;

import com.stockline.common.util.CommonValidationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/auth/login. No format check on the email: a wrong one simply doesn't match any user.
 * The password's maximum follows BCrypt, which reads at most 72 bytes of a password.
 */
public record LoginRequest(
		@NotBlank @Size(max = CommonValidationUtils.EMAIL_MAX_LENGTH) String email,
		@NotBlank @Size(max = 72) String password
) {
}
