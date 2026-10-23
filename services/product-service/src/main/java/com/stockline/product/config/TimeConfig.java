package com.stockline.product.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * The one source of "now" for the whole service. Code asks this {@link Clock} instead of calling
 * {@code Instant.now()}, so a test can replace it with {@code Clock.fixed(...)} and control the time.
 *
 * <p>UTC: we store and return {@code Instant}s, which have no time zone. The business time zone
 * (Asia/Ho_Chi_Minh) only matters when turning a date into a range, see {@code DateTimeUtils}.
 */
@Configuration(proxyBeanMethods = false)
public class TimeConfig {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}
}
