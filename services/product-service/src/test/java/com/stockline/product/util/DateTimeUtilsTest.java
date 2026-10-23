package com.stockline.product.util;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vietnam is UTC+7 all year (no daylight saving), so 00:00 in Vietnam is 17:00 UTC the day before.
 */
class DateTimeUtilsTest {

	@Test
	void toBusinessDate_lateEveningUtc_isNextDayInVietnam() {
		// 18:30 UTC on 21 October = 01:30 on 22 October in Vietnam
		assertThat(DateTimeUtils.toBusinessDate(Instant.parse("2026-10-21T18:30:00Z")))
				.isEqualTo(LocalDate.of(2026, 10, 22));
		assertThat(DateTimeUtils.toBusinessDate(Instant.parse("2026-10-21T16:59:59Z")))
				.isEqualTo(LocalDate.of(2026, 10, 21));
	}

	@Test
	void today_usesTheGivenClock() {
		Clock clock = Clock.fixed(Instant.parse("2026-12-31T17:00:00Z"), ZoneOffset.UTC);

		assertThat(DateTimeUtils.today(clock)).isEqualTo(LocalDate.of(2027, 1, 1));
	}

	@Test
	void startOfDay_isMidnightInVietnam() {
		assertThat(DateTimeUtils.startOfDay(LocalDate.of(2026, 10, 22)))
				.isEqualTo(Instant.parse("2026-10-21T17:00:00Z"));
	}

	@Test
	void endOfDayExclusive_isStartOfNextDay() {
		// A filter "to=2026-10-31" means: before 1 November 00:00 Vietnam time
		assertThat(DateTimeUtils.endOfDayExclusive(LocalDate.of(2026, 10, 31)))
				.isEqualTo(Instant.parse("2026-10-31T17:00:00Z"));
	}

	@Test
	void monthRange_coversTheWholeVietnamMonth() {
		YearMonth october = YearMonth.of(2026, 10);

		assertThat(DateTimeUtils.startOfMonth(october)).isEqualTo(Instant.parse("2026-09-30T17:00:00Z"));
		assertThat(DateTimeUtils.endOfMonthExclusive(october)).isEqualTo(Instant.parse("2026-10-31T17:00:00Z"));
	}

	@Test
	void format_showsVietnamDateAndTime() {
		Instant instant = Instant.parse("2026-10-21T18:30:05Z");

		assertThat(DateTimeUtils.formatDate(instant)).isEqualTo("22/10/2026");
		assertThat(DateTimeUtils.formatDateTime(instant)).isEqualTo("22/10/2026 01:30:05");
	}

	@Test
	void toDatabasePrecision_dropsDigitsAfterMicroseconds() {
		assertThat(DateTimeUtils.toDatabasePrecision(Instant.parse("2026-10-23T04:24:54.450923800Z")))
				.isEqualTo(Instant.parse("2026-10-23T04:24:54.450923Z"));
	}
}
