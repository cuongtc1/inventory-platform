package com.stockline.auth.dto;

import com.stockline.auth.constant.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of PUT /api/auth/password: the signed-in user changes their own password. The current one is asked
 * again, so someone using an unattended, logged-in browser can't lock the owner out.
 */
public record ChangePasswordRequest(
		@NotBlank @Size(max = PasswordPolicy.MAX_LENGTH) String currentPassword,
		@NotBlank @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH) String newPassword
) {
}
