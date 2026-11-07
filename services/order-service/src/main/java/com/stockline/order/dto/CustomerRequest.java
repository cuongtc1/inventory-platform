package com.stockline.order.dto;

import com.stockline.common.util.CommonValidationUtils;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of POST and PUT /api/customers. Only {@code name} is required.
 */
public record CustomerRequest(
		@NotBlank @Size(max = 150) String name,
		// @Email accepts null and "", so an empty field passes; the service stores it as null
		@Email @Size(max = CommonValidationUtils.EMAIL_MAX_LENGTH) String email,
		@Pattern(regexp = CommonValidationUtils.PHONE_REGEX, message = CommonValidationUtils.PHONE_MESSAGE)
		@Size(max = CommonValidationUtils.PHONE_MAX_LENGTH) String phone
) {
}
