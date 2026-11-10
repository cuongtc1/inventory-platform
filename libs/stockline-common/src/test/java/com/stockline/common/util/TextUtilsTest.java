package com.stockline.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextUtilsTest {

	@Test
	void blankToNull_trimsText_andTurnsBlankIntoNull() {
		assertThat(TextUtils.blankToNull("  Anna Pham ")).isEqualTo("Anna Pham");
		assertThat(TextUtils.blankToNull("   ")).isNull();
		assertThat(TextUtils.blankToNull("")).isNull();
		assertThat(TextUtils.blankToNull(null)).isNull();
	}

	@Test
	void strip_removesUnicodeSpacesToo() {
		// U+2003 is an "em space"; the old trim() only removes characters up to U+0020
		assertThat(TextUtils.strip(" Cables ")).isEqualTo("Cables");
		assertThat(TextUtils.strip(null)).isNull();
	}

	@Test
	void containsPattern_lowerCasesAndEscapesWildcards() {
		assertThat(TextUtils.containsPattern("Mouse")).isEqualTo("%mouse%");
		// In Java source "\\" is one backslash: the pattern is %100\%%
		assertThat(TextUtils.containsPattern("100%")).isEqualTo("%100\\%%");
		assertThat(TextUtils.containsPattern("SO_1")).isEqualTo("%so\\_1%");
		// A backslash typed by the user is escaped too: a\b becomes %a\\b%
		assertThat(TextUtils.containsPattern("a\\b")).isEqualTo("%a\\\\b%");
	}
}
