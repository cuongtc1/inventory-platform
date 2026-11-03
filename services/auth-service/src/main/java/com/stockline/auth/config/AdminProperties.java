package com.stockline.auth.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * {@code stockline.admin.*} from application.yml: the first ADMIN account, created by
 * {@link AdminAccountInitializer} when the users table is empty. Env {@code ADMIN_EMAIL}, {@code ADMIN_PASSWORD},
 * {@code ADMIN_FULL_NAME}; the defaults are for local development only.
 */
@Validated
@ConfigurationProperties("stockline.admin")
public record AdminProperties(
		@NotBlank @Email String email,
		@NotBlank String password,
		@NotBlank String fullName
) {
}
