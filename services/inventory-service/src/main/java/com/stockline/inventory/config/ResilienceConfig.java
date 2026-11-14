package com.stockline.inventory.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

/**
 * Switches on Spring Framework 7's {@code @Retryable} (and {@code @ConcurrencyLimit}): a method annotated with it is
 * called again when it throws one of the listed exceptions. Built into spring-context; the older Spring Retry library
 * isn't needed. Used for conflicts on stock levels (StockRetry).
 */
@Configuration(proxyBeanMethods = false)
@EnableResilientMethods
public class ResilienceConfig {
}
