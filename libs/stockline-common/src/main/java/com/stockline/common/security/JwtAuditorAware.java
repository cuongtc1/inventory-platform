package com.stockline.common.security;

import com.stockline.common.constant.AuditConstants;
import com.stockline.common.constant.JwtClaimNames;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;

/**
 * Who changed a row: the email from the request's token, so {@code created_by} / {@code updated_by} name the
 * user. Without a token (startup, jobs, Kafka consumers later) it is "system".
 */
public class JwtAuditorAware implements AuditorAware<String> {

	@Override
	public Optional<String> getCurrentAuditor() {
		// SecurityContextHolder: where Spring Security keeps the current request's user (per thread)
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication instanceof JwtAuthenticationToken token) {
			return Optional.ofNullable(token.getToken().getClaimAsString(JwtClaimNames.EMAIL));
		}
		return Optional.of(AuditConstants.SYSTEM_USER);
	}
}
