package com.stockline.common.autoconfigure;

import com.stockline.common.exception.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

/**
 * Registers the {@link GlobalExceptionHandler}, so every Spring MVC service returns errors in the same JSON
 * format. Only in a servlet web application: the gateway (reactive) and non-web code don't get it.
 */
@AutoConfiguration(after = CommonTimeAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CommonWebAutoConfiguration {

	// A service that needs different error handling defines its own GlobalExceptionHandler bean (e.g. a subclass)
	@Bean
	@ConditionalOnMissingBean(GlobalExceptionHandler.class)
	GlobalExceptionHandler globalExceptionHandler(MessageSource messageSource, Clock clock) {
		return new GlobalExceptionHandler(messageSource, clock);
	}
}
