package com.stockline.common.autoconfigure;

import com.stockline.common.constant.AuditConstants;
import com.stockline.common.util.DateTimeUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
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

	/**
	 * "Now" for the audit timestamps comes from the {@link Clock} bean, cut to microseconds like DATETIME(6).
	 */
	@Bean
	@ConditionalOnMissingBean(name = "auditDateTimeProvider")
	DateTimeProvider auditDateTimeProvider(Clock clock) {
		return () -> Optional.of(DateTimeUtils.toDatabasePrecision(clock.instant()));
	}

	/**
	 * Who made the change, stored in created_by / updated_by. There is no login until Phase 3 (auth-service + JWT);
	 * then a service defines its own {@code auditorAware} bean that reads the user from the token, and this one
	 * steps aside.
	 */
	@Bean
	@ConditionalOnMissingBean(name = "auditorAware")
	AuditorAware<String> auditorAware() {
		return () -> Optional.of(AuditConstants.SYSTEM_USER);
	}
}
