package com.stockline.product.config;

import com.stockline.product.util.DateTimeUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.Clock;
import java.util.Optional;

/**
 * Spring Data JPA auditing: fills {@code @CreatedDate}, {@code @LastModifiedDate}, {@code @CreatedBy} and
 * {@code @LastModifiedBy} in {@code BaseEntity} on every insert and update.
 *
 * <p>Kept out of the main application class on purpose: test slices like {@code @WebMvcTest} would otherwise
 * try to start JPA auditing without a database. {@code @DataJpaTest}s import this class.
 */
@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(dateTimeProviderRef = "auditDateTimeProvider", auditorAwareRef = "auditorAware")
@Import(TimeConfig.class)
public class JpaAuditingConfig {

	/**
	 * Who made the change, stored in created_by / updated_by. There is no login until Phase 3
	 * (auth-service + JWT); then this returns the user from the token.
	 */
	public static final String SYSTEM_USER = "system";

	/**
	 * "Now" for the audit timestamps comes from the {@link Clock} bean, cut to microseconds like DATETIME(6).
	 */
	@Bean
	DateTimeProvider auditDateTimeProvider(Clock clock) {
		return () -> Optional.of(DateTimeUtils.toDatabasePrecision(clock.instant()));
	}

	@Bean
	AuditorAware<String> auditorAware() {
		return () -> Optional.of(SYSTEM_USER);
	}
}
