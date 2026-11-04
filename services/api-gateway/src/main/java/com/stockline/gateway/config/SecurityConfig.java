package com.stockline.gateway.config;

import com.stockline.common.security.JwtSecurity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

/**
 * What happens to a request before it is forwarded (docs/conventions.md section 11):
 *
 * <ol>
 *   <li><b>CORS:</b> a browser page from an allowed origin may call the API.</li>
 *   <li><b>JWT:</b> every request needs a valid token, except login and the health check. A bad one gets a 401
 *       here and never reaches a service.</li>
 * </ol>
 *
 * <p>The gateway only checks <em>who</em> is calling. <em>What they may do</em> depends on the resource and is
 * decided by each service: the role rules are there (T3.4). The {@code Authorization} header is forwarded unchanged,
 * so a service can read the same token.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

	@Bean
	JwtDecoder jwtDecoder(JwtProperties properties, Clock clock) {
		return JwtSecurity.jwtDecoder(properties.secret(), properties.issuer(), clock);
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http,
			@Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) throws Exception {
		AuthenticationEntryPoint notLoggedIn = JwtSecurity.notLoggedIn(exceptionResolver);

		http
				// Uses the corsConfigurationSource bean below. Must happen in the security chain: a browser's preflight
				// request (OPTIONS) carries no token, so it has to be answered before the token check
				.cors(Customizer.withDefaults())
				// No cookies or sessions: the token is in a header, so CSRF can't happen
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
						.requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
						// Spring Boot forwards errors to /error; without this, an error would become a 401
						.requestMatchers("/error").permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(resourceServer -> resourceServer
						.jwt(Customizer.withDefaults())
						.authenticationEntryPoint(notLoggedIn))
				.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(notLoggedIn));
		return http.build();
	}

	/**
	 * CORS: browsers block a page from calling another origin (scheme + host + port) unless that server allows it.
	 * The frontend on localhost:5173 calling the gateway on :8080 is such a call. In development the Vite proxy
	 * avoids it; a frontend deployed on its own address needs this. Only the gateway does CORS, never the services.
	 */
	@Bean
	CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(properties.allowedOrigins());
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT));
		// Lets the frontend read Location after a POST
		configuration.setExposedHeaders(List.of(HttpHeaders.LOCATION));
		// How long a browser may remember a preflight answer
		configuration.setMaxAge(Duration.ofHours(1));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", configuration);
		return source;
	}
}
