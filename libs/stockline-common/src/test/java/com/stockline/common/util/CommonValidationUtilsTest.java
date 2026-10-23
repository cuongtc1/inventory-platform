package com.stockline.common.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code @ParameterizedTest} runs the same test once per value (JUnit 5).
 */
class CommonValidationUtilsTest {

	@ParameterizedTest
	@ValueSource(strings = {"+84 90 111 2233", "(028) 3800-1122", "0901112233", ""})
	void isValidPhone_digitsSpacesAndSigns_areValid(String phone) {
		assertThat(CommonValidationUtils.isValidPhone(phone)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = {"call me", "090-ABC", "+84#90"})
	void isValidPhone_letterOrOtherSign_isInvalid(String phone) {
		assertThat(CommonValidationUtils.isValidPhone(phone)).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = {"A01", "KB-104", "WH_01", "v1.2"})
	void isValidCode_lettersDigitsDotUnderscoreDash_areValid(String code) {
		assertThat(CommonValidationUtils.isValidCode(code)).isTrue();
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {"", "A 01", "KB/104", "Mũi"})
	void isValidCode_emptySpaceOrOtherCharacter_isInvalid(String code) {
		assertThat(CommonValidationUtils.isValidCode(code)).isFalse();
	}
}
