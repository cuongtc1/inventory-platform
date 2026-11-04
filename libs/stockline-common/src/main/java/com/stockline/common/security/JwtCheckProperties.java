package com.stockline.common.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * {@code stockline.jwt.*} from a service's application.yml: what a token must be signed with and who must have
 * issued it. Every service that checks tokens has these two lines.
 *
 * @param secret the HMAC key; the same {@code JWT_SECRET} everywhere, or tokens from auth-service are refused
 * @param issuer the {@code iss} claim auth-service writes
 */
@Validated
@ConfigurationProperties("stockline.jwt")
public record JwtCheckProperties(
		@NotBlank String secret,
		@NotBlank String issuer
) {
}
