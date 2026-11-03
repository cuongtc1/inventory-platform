package com.stockline.auth.service;

import com.stockline.auth.config.JwtProperties;
import com.stockline.auth.entity.User;
import com.stockline.common.constant.JwtClaimNames;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

/**
 * Creates the JWT a user gets at login (docs/conventions.md section 11).
 *
 * <p>A JWT is three Base64 parts joined by dots: header ({@code {"alg":"HS256"}}), claims (the JSON below) and a
 * signature over the first two. Anyone can read the claims, so they never contain secrets; only someone with
 * the key can produce a valid signature, so nobody can change the role in a token without it being noticed.
 */
@Service
public class TokenService {

	private final JwtEncoder jwtEncoder;
	private final JwtProperties properties;
	private final Clock clock;

	public TokenService(JwtEncoder jwtEncoder, JwtProperties properties, Clock clock) {
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
		this.clock = clock;
	}

	/**
	 * A signed token for this user, valid for {@code stockline.jwt.expiration} from now:
	 * <pre>
	 * { "iss": "stockline-auth", "sub": "1", "email": "admin@stockline.local", "name": "Stockline Admin",
	 *   "role": "ADMIN", "iat": 1762149600, "exp": 1762178400 }
	 * </pre>
	 */
	public Jwt createToken(User user) {
		Instant now = clock.instant();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(properties.issuer())
				// "sub" (subject) is a string in the JWT standard
				.subject(String.valueOf(user.getId()))
				.issuedAt(now)
				.expiresAt(now.plus(properties.expiration()))
				.claim(JwtClaimNames.EMAIL, user.getEmail())
				.claim(JwtClaimNames.NAME, user.getFullName())
				.claim(JwtClaimNames.ROLE, user.getUserType().name())
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims));
	}
}
