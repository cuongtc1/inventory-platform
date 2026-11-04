package com.stockline.common.autoconfigure;

import com.stockline.common.exception.DataExceptionHandler;
import com.stockline.common.exception.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Registers the {@link GlobalExceptionHandler}, so every Spring MVC service, including the gateway, returns errors
 * in the same JSON format. Only in a servlet web application: non-web code doesn't get it.
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

	/**
	 * Only for services with Hibernate and Spring Data. A nested class with class names as text: Spring checks
	 * them before loading anything, so a service without those jars (the api-gateway) never touches this code.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = {
			"org.hibernate.exception.ConstraintViolationException",
			"org.springframework.dao.DataIntegrityViolationException",
			"org.springframework.data.core.PropertyReferenceException"
	})
	static class DataErrors {

		@Bean
		@ConditionalOnMissingBean(DataExceptionHandler.class)
		DataExceptionHandler dataExceptionHandler(GlobalExceptionHandler handler, MessageSource messageSource) {
			return new DataExceptionHandler(handler, messageSource);
		}
	}
}
