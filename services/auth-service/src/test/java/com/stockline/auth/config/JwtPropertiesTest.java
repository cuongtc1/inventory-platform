package com.stockline.auth.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

	@Test
	void secretShorterThan32Bytes_isRejectedAtStartup() {
		assertThatThrownBy(() -> new JwtProperties("too-short", Duration.ofHours(8), "stockline-auth"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("at least 32 bytes");
	}

	@Test
	void secretOf32Bytes_isAccepted() {
		assertThat(new JwtProperties("x".repeat(32), Duration.ofHours(8), "stockline-auth").secret()).hasSize(32);
	}
}
