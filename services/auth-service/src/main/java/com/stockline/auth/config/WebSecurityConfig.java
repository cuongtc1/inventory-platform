package com.stockline.auth.config;

import com.stockline.auth.constant.UserType;
import com.stockline.common.security.JwtSecurity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Who may call what (docs/conventions.md section 11). Spring Security runs as a chain of servlet filters
 * before any controller: it reads {@code Authorization: Bearer <token>}, checks the token with the
 * {@code JwtDecoder} bean, and then applies the rules below.
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
		AuthenticationEntryPoint notLoggedIn = JwtSecurity.notLoggedIn(exceptionResolver);
		AccessDeniedHandler noPermission = JwtSecurity.noPermission(exceptionResolver);

		http
				// CSRF protection is for browsers that send cookies automatically. This API uses no cookies or
				// sessions: the token is in a header the browser never adds by itself, so CSRF can't happen.
				.csrf(csrf -> csrf.disable())
				// No HTTP session: every request proves who it is with its token (stateless)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
						.requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
						.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
						// Spring Boot forwards errors to /error; without this, an error would become a 401
						.requestMatchers("/error").permitAll()
						.requestMatchers("/api/users/**").hasRole(UserType.ADMIN.name())
						.anyRequest().authenticated())
				.oauth2ResourceServer(resourceServer -> resourceServer
						.jwt(jwt -> jwt.jwtAuthenticationConverter(JwtSecurity.roleConverter()))
						// A bad or expired token: answered by our handler, so the JSON is the usual error format
						.authenticationEntryPoint(notLoggedIn)
						.accessDeniedHandler(noPermission))
				.exceptionHandling(exceptions -> exceptions
						// No token at all
						.authenticationEntryPoint(notLoggedIn)
						.accessDeniedHandler(noPermission));
		return http.build();
	}
}
