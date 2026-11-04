package com.stockline.common.security;

import com.stockline.common.constant.JwtClaimNames;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtSecurityTest {

	private static final String SECRET = "test-secret-that-is-at-least-32-bytes-long";
	private static final String ISSUER = "stockline-auth";
	private static final Instant NOW = Instant.parse("2026-11-04T08:00:00Z");

	private final JwtDecoder decoder = JwtSecurity.jwtDecoder(SECRET, ISSUER, Clock.fixed(NOW, ZoneOffset.UTC));

	@Test
	void secretKey_shorterThan32Bytes_isRejected() {
		assertThatThrownBy(() -> JwtSecurity.secretKey("too-short"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("at least 32 bytes");
	}

	@Test
	void jwtDecoder_validToken_isAccepted() {
		assertThat(decoder.decode(token(SECRET, ISSUER, NOW.plusSeconds(60))).getSubject()).isEqualTo("7");
	}

	@Test
	void jwtDecoder_wrongSecret_isRejected() {
		String token = token("another-secret-that-is-at-least-32-bytes", ISSUER, NOW.plusSeconds(60));
		assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(BadJwtException.class);
	}

	@Test
	void jwtDecoder_wrongIssuer_isRejected() {
		String token = token(SECRET, "someone-else", NOW.plusSeconds(60));
		assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(BadJwtException.class);
	}

	@Test
	void jwtDecoder_expiredToken_isRejectedByTheClock() {
		// JwtTimestampValidator allows 60 seconds of clock difference between servers
		String token = token(SECRET, ISSUER, NOW.minus(Duration.ofMinutes(2)));
		assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(BadJwtException.class);
	}

	@Test
	void roleConverter_roleClaim_becomesRoleAuthority() {
		// when
		Jwt jwt = decoder.decode(token(SECRET, ISSUER, NOW.plusSeconds(60)));
		AbstractAuthenticationToken authentication = JwtSecurity.roleConverter().convert(jwt);

		// then
		assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
				.contains("ROLE_MANAGER");
	}

	private static String token(String secret, String issuer, Instant expiresAt) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(issuer)
				.subject("7")
				.issuedAt(expiresAt.minusSeconds(600))
				.expiresAt(expiresAt)
				.claim(JwtClaimNames.ROLE, "MANAGER")
				.build();
		return NimbusJwtEncoder.withSecretKey(JwtSecurity.secretKey(secret)).algorithm(MacAlgorithm.HS256).build()
				.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
				.getTokenValue();
	}
}
