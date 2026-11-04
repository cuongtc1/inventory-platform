package com.stockline.auth.config;

import com.stockline.common.security.JwtSecurity;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;

/**
 * The tools only auth-service needs: a password hasher and a token signer. The token checker comes from
 * stockline-common, and which URL needs what is in {@link WebSecurityConfig}.
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
	 * Signs tokens with HMAC-SHA256 (HS256) and the shared secret: whoever knows the secret (the api-gateway and
	 * the services) can check that a token is genuine and unchanged. Spring Security's {@code NimbusJwtEncoder} does
	 * the JSON, Base64 and signing work.
	 */
	@Bean
	public JwtEncoder jwtEncoder(JwtProperties properties) {
		return NimbusJwtEncoder.withSecretKey(secretKey(properties)).algorithm(MacAlgorithm.HS256).build();
	}

	/** The JWT_SECRET text as an HMAC key. Also used by tests to check tokens. */
	public static SecretKey secretKey(JwtProperties properties) {
		return JwtSecurity.secretKey(properties.secret());
	}
}
