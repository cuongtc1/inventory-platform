package com.stockline.product.entity;

import com.stockline.common.constant.AuditConstants;
import com.stockline.product.TestcontainersConfiguration;
import com.stockline.product.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static com.stockline.product.TestData.category;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * JPA auditing fills created_at/updated_at from the Clock bean and created_by/updated_by from the
 * AuditorAware bean. The test replaces the Clock with one it can move forward.
 */
@DataJpaTest
@Import({TestcontainersConfiguration.class, AuditingTest.ClockConfig.class})
class AuditingTest {

	private static final Instant START = Instant.parse("2026-10-23T08:00:00.123456789Z");

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private MovableClock clock;

	@Test
	void save_newEntity_setsAllAuditFieldsFromClockAndAuditor() {
		// given
		clock.setInstant(START);

		// when
		Category saved = categoryRepository.saveAndFlush(category("Cables"));

		// then: cut to microseconds like DATETIME(6)
		assertThat(saved.getCreatedAt()).isEqualTo(Instant.parse("2026-10-23T08:00:00.123456Z"));
		assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
		assertThat(saved.getCreatedBy()).isEqualTo(AuditConstants.SYSTEM_USER);
		assertThat(saved.getUpdatedBy()).isEqualTo(AuditConstants.SYSTEM_USER);
	}

	@Test
	void update_changesUpdatedAtOnly() {
		// given
		clock.setInstant(START);
		Category saved = categoryRepository.saveAndFlush(category("Cables"));
		Instant createdAt = saved.getCreatedAt();

		// when: one hour later
		clock.setInstant(START.plus(Duration.ofHours(1)));
		saved.setName("USB Cables");
		categoryRepository.saveAndFlush(saved);

		// then
		assertThat(saved.getCreatedAt()).isEqualTo(createdAt);
		assertThat(saved.getUpdatedAt()).isEqualTo(Instant.parse("2026-10-23T09:00:00.123456Z"));
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class ClockConfig {

		// @Primary: wins over the normal Clock bean from TimeConfig wherever a Clock is injected
		@Bean
		@Primary
		MovableClock movableClock() {
			return new MovableClock();
		}
	}

	/**
	 * A Clock whose time the test sets. Clock.fixed() can't change after it is created.
	 */
	static class MovableClock extends Clock {

		private Instant instant = Instant.EPOCH;

		void setInstant(Instant instant) {
			this.instant = instant;
		}

		@Override
		public Instant instant() {
			return instant;
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			throw new UnsupportedOperationException();
		}
	}
}
