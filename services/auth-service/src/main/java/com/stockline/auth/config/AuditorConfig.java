package com.stockline.auth.config;

import com.stockline.common.constant.AuditConstants;
import com.stockline.common.constant.JwtClaimNames;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;

/**
 * Who changed a row: replaces the library's {@code auditorAware} bean (always "system") with the email from the
 * request's token, so {@code created_by} / {@code updated_by} of users show which ADMIN made the change.
 * Without a token (startup, the admin initializer, login) it stays "system".
 *
 * <p>The other services get the same rule in T3.4; then this moves to a shared place.
 */
@Configuration(proxyBeanMethods = false)
public class AuditorConfig {

	@Bean
	AuditorAware<String> auditorAware() {
		return () -> {
			// SecurityContextHolder: where Spring Security keeps the current request's user (per thread)
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			if (authentication instanceof JwtAuthenticationToken token) {
				return Optional.ofNullable(token.getToken().getClaimAsString(JwtClaimNames.EMAIL));
			}
			return Optional.of(AuditConstants.SYSTEM_USER);
		};
	}
}
