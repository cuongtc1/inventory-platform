package com.stockline.auth.config;

import com.stockline.common.security.JwtSecurity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * {@code stockline.jwt.*} from application.yml: how tokens are signed and how long they are valid.
 *
 * <p>A {@code record} works as a {@code @ConfigurationProperties} class since Spring Boot 3: Spring calls its
 * constructor with the values (no setters needed). {@code @Validated} checks the annotations at startup, so a
 * missing value stops the application with a clear message instead of failing at the first login.
 *
 * @param secret     the HMAC key shared with everyone who checks tokens (api-gateway); env {@code JWT_SECRET}
 * @param expiration how long a token is valid, e.g. {@code 8h}; env {@code JWT_EXPIRATION}
 * @param issuer     the {@code iss} claim, so a checker knows the token comes from auth-service
 */
@Validated
@ConfigurationProperties("stockline.jwt")
public record JwtProperties(
		@NotBlank String secret,
		@NotNull Duration expiration,
		@NotBlank String issuer
) {

	public JwtProperties {
		// A compact constructor (records, Java 16): runs before the fields are set, for checks like this one
		if (secret != null) {
			JwtSecurity.secretKey(secret);
		}
	}
}
