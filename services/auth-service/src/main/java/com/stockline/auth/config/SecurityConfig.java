package com.stockline.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

/**
 * The security tools: a password hasher, a token signer (login) and a token checker (every other request).
 * Which URL needs what is in {@link WebSecurityConfig}.
 */
@Configuration(proxyBeanMethods = false)
// Registers JwtProperties here too, not only through @ConfigurationPropertiesScan on the application class:
// @WebMvcTest slices import this class but don't run that scan
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

	/**
	 * BCrypt: a slow, salted hash made for passwords. Each hash includes its own random salt and the cost factor
	 * ("$2a$10$..."), so the same password gives a different hash every time, and {@code matches} still works.
	 */
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/**
	 * Signs tokens with HMAC-SHA256 (HS256) and the shared secret: whoever knows the secret (the api-gateway in
	 * T3.3) can check that a token is genuine and unchanged. Spring Security's {@code NimbusJwtEncoder} does the
	 * JSON, Base64 and signing work.
	 */
	@Bean
	public JwtEncoder jwtEncoder(JwtProperties properties) {
		return NimbusJwtEncoder.withSecretKey(secretKey(properties)).algorithm(MacAlgorithm.HS256).build();
	}

	/**
	 * Checks the token of each request: the signature (same secret, HS256), the expiry against the {@code Clock}
	 * bean (tests can move time), and that {@code iss} is ours. A token that fails any check is a 401.
	 */
	@Bean
	public JwtDecoder jwtDecoder(JwtProperties properties, Clock clock) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey(properties))
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		JwtTimestampValidator expiry = new JwtTimestampValidator();
		expiry.setClock(clock);
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(expiry, new JwtIssuerValidator(properties.issuer())));
		return decoder;
	}

	/** The JWT_SECRET text as an HMAC key. Also used by tests to check tokens. */
	public static SecretKey secretKey(JwtProperties properties) {
		return new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}
}
