package com.stockline.common.util;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Date and time rules for Stockline (docs/conventions.md section 1).
 *
 * <ul>
 *   <li>Every moment is an {@link Instant}: a point on the UTC timeline, no time zone. Stored as DATETIME(6) in
 *       UTC, sent in JSON as ISO-8601 with "Z" ({@code 2026-10-22T08:15:30Z}).</li>
 *   <li>The business works in Vietnam time ({@link #BUSINESS_ZONE}). "Today", "October" or a date filter like
 *       {@code ?from=2026-10-01&to=2026-10-31} mean Vietnam days, so they are turned into Instant ranges here.</li>
 *   <li>Ranges are half-open, {@code [from, to)}: a moment belongs to a day if it is at or after the day's start
 *       and before the next day's start. No "23:59:59.999" tricks.</li>
 * </ul>
 *
 * <p>Example: an order placed at {@code 2026-10-21T18:30:00Z} is on 22 October in Vietnam (UTC+7, 01:30).
 */
public final class DateTimeUtils {

	public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

	/** For texts people read: exports, logs, e-mails. The API itself always uses ISO-8601. */
	public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	public static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

	private DateTimeUtils() {
	}

	/** The Vietnam date of a moment. */
	public static LocalDate toBusinessDate(Instant instant) {
		return instant.atZone(BUSINESS_ZONE).toLocalDate();
	}

	/** Today in Vietnam. Pass the {@link Clock} bean so tests can fix "today". */
	public static LocalDate today(Clock clock) {
		return toBusinessDate(clock.instant());
	}

	/** Midnight at the start of a Vietnam day, as a moment: 22/10/2026 → {@code 2026-10-21T17:00:00Z}. */
	public static Instant startOfDay(LocalDate date) {
		return date.atStartOfDay(BUSINESS_ZONE).toInstant();
	}

	/**
	 * End of a date filter: the start of the next day, used with "&lt;". So {@code to=2026-10-31} includes
	 * everything on 31 October.
	 */
	public static Instant endOfDayExclusive(LocalDate date) {
		return startOfDay(date.plusDays(1));
	}

	/** Start of a Vietnam month, e.g. for "sales by month". */
	public static Instant startOfMonth(YearMonth month) {
		return startOfDay(month.atDay(1));
	}

	/** End of a Vietnam month, exclusive: the start of the next month. */
	public static Instant endOfMonthExclusive(YearMonth month) {
		return startOfMonth(month.plusMonths(1));
	}

	/** {@code 2026-10-21T18:30:00Z} → "22/10/2026" (Vietnam date). */
	public static String formatDate(Instant instant) {
		return DATE_FORMAT.format(instant.atZone(BUSINESS_ZONE));
	}

	/** {@code 2026-10-21T18:30:00Z} → "22/10/2026 01:30:00" (Vietnam time). */
	public static String formatDateTime(Instant instant) {
		return DATE_TIME_FORMAT.format(instant.atZone(BUSINESS_ZONE));
	}

	/**
	 * Cuts a moment to microseconds, the precision of DATETIME(6). Without this, MySQL rounds the extra digits
	 * and the value read back differs from the one saved.
	 */
	public static Instant toDatabasePrecision(Instant instant) {
		return instant.truncatedTo(ChronoUnit.MICROS);
	}
}
