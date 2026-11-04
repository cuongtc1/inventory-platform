package com.stockline.gateway.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * {@code stockline.jwt.*} from application.yml: what a token must be signed with and who must have issued it.
 *
 * @param secret the HMAC key; must be the same {@code JWT_SECRET} as auth-service's
 * @param issuer the {@code iss} claim auth-service writes
 */
@Validated
@ConfigurationProperties("stockline.jwt")
public record JwtProperties(
		@NotBlank String secret,
		@NotBlank String issuer
) {
}
