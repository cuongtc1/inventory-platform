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
}
