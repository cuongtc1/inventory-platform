package com.stockline.common.autoconfigure;

import com.stockline.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks which beans the auto-configurations create, without starting a whole application.
 * {@code ApplicationContextRunner} builds a small Spring context per test with exactly the given configurations.
 */
class CommonAutoConfigurationTest {

	private final ApplicationContextRunner plainContext = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					CommonTimeAutoConfiguration.class, CommonWebAutoConfiguration.class));

	private final WebApplicationContextRunner webContext = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					CommonTimeAutoConfiguration.class, CommonWebAutoConfiguration.class));

	@Test
	void clock_isUtcSystemClock() {
		plainContext.run(context -> assertThat(context.getBean(Clock.class).getZone()).isEqualTo(ZoneOffset.UTC));
	}

	@Test
	void clock_definedByTheService_replacesTheDefault() {
		Clock fixed = Clock.fixed(Instant.parse("2026-10-23T08:00:00Z"), ZoneOffset.UTC);

		plainContext.withBean(Clock.class, () -> fixed)
				.run(context -> assertThat(context.getBean(Clock.class)).isSameAs(fixed));
	}

	@Test
	void exceptionHandler_onlyInAServletWebApplication() {
		webContext.run(context -> assertThat(context).hasSingleBean(GlobalExceptionHandler.class));
		plainContext.run(context -> assertThat(context).doesNotHaveBean(GlobalExceptionHandler.class));
	}
}
