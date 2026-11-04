package com.stockline.auth.config;

import com.stockline.common.constant.UserType;
import com.stockline.common.security.JwtSecurity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Who may call what (docs/conventions.md section 11). Spring Security runs as a chain of servlet filters
 * before any controller: it reads {@code Authorization: Bearer <token>}, checks the token with the
 * {@code JwtDecoder} bean from stockline-common, and then applies the rules below.
 *
 * <ul>
 *   <li>Without a token: only login, health and the API docs.</li>
 *   <li>{@code /api/users/**}: ADMIN only (403 for other roles).</li>
 *   <li>Everything else ({@code /api/auth/me}, {@code /api/auth/password}): any valid token.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class WebSecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http,
			@Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) throws Exception {
		JwtSecurity.configure(http, exceptionResolver)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
						.requestMatchers(JwtSecurity.OPEN_PATHS).permitAll()
						.requestMatchers("/api/users/**").hasRole(UserType.ADMIN.name())
						.anyRequest().authenticated());
		return http.build();
	}
}
