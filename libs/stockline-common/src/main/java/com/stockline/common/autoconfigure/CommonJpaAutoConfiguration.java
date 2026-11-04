package com.stockline.common.autoconfigure;

import com.stockline.common.constant.AuditConstants;
import com.stockline.common.security.JwtAuditorAware;
import com.stockline.common.util.DateTimeUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.Clock;
import java.util.Optional;

/**
 * Spring Data JPA auditing: fills {@code @CreatedDate}, {@code @LastModifiedDate}, {@code @CreatedBy} and
 * {@code @LastModifiedBy} in {@code BaseEntity} on every insert and update.
 *
 * <p>Only active when Spring Data JPA is on the classpath, so a service without a database (e.g. the gateway)
 * can use the library too.
 */
@AutoConfiguration(after = CommonTimeAutoConfiguration.class)
@ConditionalOnClass(AuditingEntityListener.class)
@EnableJpaAuditing(dateTimeProviderRef = "auditDateTimeProvider", auditorAwareRef = "auditorAware")
public class CommonJpaAutoConfiguration {

	private static final String JWT_AUTHENTICATION =
			"org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken";

	/**
	 * "Now" for the audit timestamps comes from the {@link Clock} bean, cut to microseconds like DATETIME(6).
	 */
	@Bean
	@ConditionalOnMissingBean(name = "auditDateTimeProvider")
	DateTimeProvider auditDateTimeProvider(Clock clock) {
		return () -> Optional.of(DateTimeUtils.toDatabasePrecision(clock.instant()));
	}

	/** Who made the change, stored in created_by / updated_by: the user from the token. */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = JWT_AUTHENTICATION)
	static class TokenAuditor {

		@Bean
		@ConditionalOnMissingBean(name = "auditorAware")
		AuditorAware<String> auditorAware() {
			return new JwtAuditorAware();
		}
	}

	/** A service without Spring Security has no user to name. */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingClass(JWT_AUTHENTICATION)
	static class SystemAuditor {

		@Bean
		@ConditionalOnMissingBean(name = "auditorAware")
		AuditorAware<String> auditorAware() {
			return () -> Optional.of(AuditConstants.SYSTEM_USER);
		}
	}
}
