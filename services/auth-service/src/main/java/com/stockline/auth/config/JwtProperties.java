package com.stockline.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
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

	/** HS256 needs a key of at least 256 bits (RFC 7518); a shorter one is rejected by the signer anyway. */
	public static final int MIN_SECRET_BYTES = 32;

	public JwtProperties {
		// A compact constructor (records, Java 16): runs before the fields are set, for checks like this one
		if (secret != null && secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
			throw new IllegalArgumentException(
					"stockline.jwt.secret (JWT_SECRET) must be at least " + MIN_SECRET_BYTES + " bytes long");
		}
	}
}
