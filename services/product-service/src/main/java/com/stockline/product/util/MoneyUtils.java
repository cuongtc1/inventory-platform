package com.stockline.product.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Currency;
import java.util.Locale;

/**
 * Money rules for Stockline (docs/conventions.md section 1): one currency, USD; amounts are {@link BigDecimal}
 * with 2 decimals, stored as DECIMAL(12,2). Never {@code double}: 0.1 + 0.2 is 0.30000000000000004 in binary.
 *
 * <p>Calculations (price × quantity, totals, discounts) keep full precision and round once, at the end, with
 * {@link #ROUNDING} (half up: 2.345 → 2.35, 2.344 → 2.34).
 */
public final class MoneyUtils {

	public static final Currency CURRENCY = Currency.getInstance("USD");
	/** 2 for USD (cents). Taken from the currency so it can't drift from it. */
	public static final int SCALE = CURRENCY.getDefaultFractionDigits();
	public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
	public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, ROUNDING);

	private MoneyUtils() {
	}

	/**
	 * An amount with exactly 2 decimals: 40 → 40.00, 2.345 → 2.35. Use before saving or returning an amount, so
	 * the response to a save matches what DECIMAL(12,2) gives back later. {@code null} stays {@code null}.
	 */
	public static BigDecimal toMoney(BigDecimal amount) {
		return amount == null ? null : amount.setScale(SCALE, ROUNDING);
	}

	/** Line total: unit price × quantity, rounded once. */
	public static BigDecimal multiply(BigDecimal unitPrice, int quantity) {
		return toMoney(unitPrice.multiply(BigDecimal.valueOf(quantity)));
	}

	/** Sum of amounts; {@link #ZERO} for an empty collection. */
	public static BigDecimal sum(Collection<BigDecimal> amounts) {
		return toMoney(amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
	}

	/** For texts people read (exports, e-mails): 1234.5 → "$1,234.50". The API sends plain numbers. */
	public static String format(BigDecimal amount) {
		NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
		format.setCurrency(CURRENCY);
		return format.format(toMoney(amount));
	}
}
