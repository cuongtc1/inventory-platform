package com.stockline.common.security;

import com.stockline.common.constant.CommonMessageKeys;
import com.stockline.common.constant.JwtClaimNames;
import com.stockline.common.exception.ForbiddenException;
import com.stockline.common.exception.UnauthorizedException;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

/**
 * The parts of checking a Stockline JWT that are the same everywhere (docs/conventions.md section 11): the key,
 * the token checks, the {@code role} claim and the 401/403 answers. Each service still writes its own
 * {@code SecurityFilterChain}, because which URL needs what differs.
 *
 * <p>Needs {@code spring-boot-starter-security-oauth2-resource-server} in the service; it is an optional
 * dependency of this library, so services without security don't get it.
 */
public final class JwtSecurity {

	/** HS256 needs a key of at least 256 bits (RFC 7518). */
	public static final int MIN_SECRET_BYTES = 32;

	/** Spring Security's convention: a role is an authority named "ROLE_" + the role, checked by hasRole(...). */
	private static final String ROLE_PREFIX = "ROLE_";

	private JwtSecurity() {
	}

	/**
	 * The JWT_SECRET text as an HMAC key.
	 *
	 * @throws IllegalArgumentException if it is too short, so a service with a bad secret doesn't start
	 */
	public static SecretKey secretKey(String secret) {
		byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
		if (bytes.length < MIN_SECRET_BYTES) {
			throw new IllegalArgumentException("JWT_SECRET must be at least " + MIN_SECRET_BYTES + " bytes long");
		}
		return new SecretKeySpec(bytes, "HmacSHA256");
	}

	/**
	 * Checks the token of each request: the signature (same secret, HS256), the expiry against the {@code Clock}
	 * (tests can move time), and that {@code iss} is ours. A token that fails any check is a 401.
	 */
	public static JwtDecoder jwtDecoder(String secret, String issuer, Clock clock) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey(secret))
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		JwtTimestampValidator expiry = new JwtTimestampValidator();
		expiry.setClock(clock);
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(expiry, new JwtIssuerValidator(issuer)));
		return decoder;
	}

	/**
	 * Turns the token's {@code role} claim ("ADMIN") into the authority "ROLE_ADMIN" that
	 * {@code hasRole("ADMIN")} looks for. Spring's default would read a {@code scope} claim instead.
	 */
	public static JwtAuthenticationConverter roleConverter() {
		JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
		authorities.setAuthoritiesClaimName(JwtClaimNames.ROLE);
		authorities.setAuthorityPrefix(ROLE_PREFIX);
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authorities);
		return converter;
	}

	/**
	 * Answers a missing, invalid or expired token with 401. Security filters run before Spring MVC, so the error
	 * is handed to the {@code handlerExceptionResolver}: it reaches {@code GlobalExceptionHandler}, and the JSON
	 * is the usual error format.
	 */
	public static AuthenticationEntryPoint notLoggedIn(HandlerExceptionResolver exceptionResolver) {
		return (request, response, ex) -> exceptionResolver.resolveException(request, response, null,
				new UnauthorizedException(CommonMessageKeys.AUTH_UNAUTHORIZED));
	}

	/** Answers a valid token without the needed role with 403, the same way as {@link #notLoggedIn}. */
	public static AccessDeniedHandler noPermission(HandlerExceptionResolver exceptionResolver) {
		return (request, response, ex) -> exceptionResolver.resolveException(request, response, null,
				new ForbiddenException(CommonMessageKeys.AUTH_FORBIDDEN));
	}
}
