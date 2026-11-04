package com.stockline.auth.service;

import com.stockline.auth.config.JwtProperties;
import com.stockline.auth.config.SecurityConfig;
import com.stockline.auth.constant.UserStatus;
import com.stockline.common.constant.JwtClaimNames;
import com.stockline.common.constant.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static com.stockline.auth.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Creates real tokens with the real encoder and reads them back with Spring Security's decoder, the same way
 * the api-gateway will (T3.3). No Spring context.
 */
class TokenServiceTest {

	private static final Instant NOW = Instant.parse("2026-11-03T08:00:00Z");
	private static final JwtProperties PROPERTIES = new JwtProperties(
			"test-secret-that-is-at-least-32-bytes-long", Duration.ofHours(8), "stockline-auth");

	private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
	private TokenService tokenService;

	@BeforeEach
	void setUp() {
		tokenService = new TokenService(new SecurityConfig().jwtEncoder(PROPERTIES), PROPERTIES, clock);
	}

	@Test
	void createToken_containsUserIdRoleAndExpiry() {
		// when
		Jwt created = tokenService.createToken(user(42L, "linh@corp.com", "hash", UserType.MANAGER, UserStatus.ACTIVE));

		// then: read back from the signed string, not from the returned object
		Jwt token = decoder(PROPERTIES.secret()).decode(created.getTokenValue());
		assertThat(token.getHeaders()).containsEntry("alg", "HS256");
		assertThat(token.getSubject()).isEqualTo("42");
		assertThat(token.getClaimAsString(JwtClaimNames.ROLE)).isEqualTo("MANAGER");
		assertThat(token.getClaimAsString(JwtClaimNames.EMAIL)).isEqualTo("linh@corp.com");
		assertThat(token.getClaimAsString(JwtClaimNames.NAME)).isEqualTo("Test manager");
		assertThat(token.getClaimAsString("iss")).isEqualTo("stockline-auth");
		assertThat(token.getIssuedAt()).isEqualTo(NOW);
		assertThat(token.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(8)));
		assertThat(created.getExpiresAt()).isEqualTo(token.getExpiresAt());
	}

	@Test
	void createToken_checkedWithAnotherSecret_isRejected() {
		// given
		String token = tokenService.createToken(user(42L, "linh@corp.com", "hash", UserType.STAFF, UserStatus.ACTIVE))
				.getTokenValue();

		// when / then: a token signed with a different key is not accepted
		assertThatThrownBy(() -> decoder("another-secret-that-is-at-least-32-bytes").decode(token))
				.isInstanceOf(BadJwtException.class);
	}

	@Test
	void createToken_withChangedRole_isRejected() {
		// given: someone edits the claims part (STAFF → ADMIN) but can't sign it again
		String[] parts = tokenService.createToken(user(42L, "a@corp.com", "hash", UserType.STAFF, UserStatus.ACTIVE))
				.getTokenValue().split("\\.");
		String claims = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
		String forgedClaims = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(claims.replace("\"STAFF\"", "\"ADMIN\"").getBytes(StandardCharsets.UTF_8));
		String forged = parts[0] + "." + forgedClaims + "." + parts[2];

		// when / then
		assertThatThrownBy(() -> decoder(PROPERTIES.secret()).decode(forged)).isInstanceOf(BadJwtException.class);
	}

	/** Checks signature and expiry, with "now" taken from the test's fixed clock. */
	private JwtDecoder decoder(String secret) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder
				.withSecretKey(SecurityConfig.secretKey(new JwtProperties(secret, Duration.ofHours(1), "test")))
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		JwtTimestampValidator timestamps = new JwtTimestampValidator();
		timestamps.setClock(clock);
		decoder.setJwtValidator(timestamps);
		return decoder;
	}
}
