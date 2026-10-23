package com.stockline.product.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyUtilsTest {

	@Test
	void currency_isUsdWithTwoDecimals() {
		assertThat(MoneyUtils.CURRENCY.getCurrencyCode()).isEqualTo("USD");
		assertThat(MoneyUtils.SCALE).isEqualTo(2);
	}

	@Test
	void toMoney_setsTwoDecimalsAndRoundsHalfUp() {
		// isEqualTo (not isEqualByComparingTo) also checks the scale: 40.00, not 40
		assertThat(MoneyUtils.toMoney(new BigDecimal("40"))).isEqualTo(new BigDecimal("40.00"));
		assertThat(MoneyUtils.toMoney(new BigDecimal("2.345"))).isEqualTo(new BigDecimal("2.35"));
		assertThat(MoneyUtils.toMoney(new BigDecimal("2.344"))).isEqualTo(new BigDecimal("2.34"));
		assertThat(MoneyUtils.toMoney(null)).isNull();
	}

	@Test
	void multiply_roundsOnceAtTheEnd() {
		// 3 × 0.335 = 1.005 → 1.01; rounding the price first (0.34) would give 1.02
		assertThat(MoneyUtils.multiply(new BigDecimal("0.335"), 3)).isEqualTo(new BigDecimal("1.01"));
	}

	@Test
	void sum_addsAmounts_andEmptyIsZero() {
		assertThat(MoneyUtils.sum(List.of(new BigDecimal("0.10"), new BigDecimal("0.20"))))
				.isEqualTo(new BigDecimal("0.30"));
		assertThat(MoneyUtils.sum(List.of())).isEqualTo(MoneyUtils.ZERO);
	}

	@Test
	void format_isUsDollarText() {
		assertThat(MoneyUtils.format(new BigDecimal("1234.5"))).isEqualTo("$1,234.50");
	}
}
