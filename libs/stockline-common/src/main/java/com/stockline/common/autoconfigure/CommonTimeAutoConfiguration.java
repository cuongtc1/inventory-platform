package com.stockline.common.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

/**
 * The one source of "now" for a service. Code asks this {@link Clock} instead of calling {@code Instant.now()},
 * so a test can replace it and control the time.
 *
 * <p>UTC: we store and return {@code Instant}s, which have no time zone. The business time zone
 * (Asia/Ho_Chi_Minh) only matters when turning a date into a range, see {@code DateTimeUtils}.
 *
 * <p>How an auto-configuration works: Spring Boot reads the class names in
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} from every jar on the
 * classpath and applies them, so a service only needs the library as a dependency. The test slices
 * ({@code @WebMvcTest}, {@code @DataJpaTest}) have their own lists in the same folder.
 */
@AutoConfiguration
public class CommonTimeAutoConfiguration {

	// @ConditionalOnMissingBean: a service (or a test) that defines its own Clock replaces this one
	@Bean
	@ConditionalOnMissingBean
	Clock clock() {
		return Clock.systemUTC();
	}
}
