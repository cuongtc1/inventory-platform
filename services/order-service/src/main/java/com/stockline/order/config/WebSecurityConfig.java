package com.stockline.order.config;

import com.stockline.common.constant.UserType;
import com.stockline.common.security.JwtSecurity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Who may do what with customers and orders (docs/conventions.md section 11). Every signed-in user reads. Staff also
 * create sales orders and mark them paid, shipped and completed, and add and edit customers: they do the daily work.
 * Cancelling an order, deleting and everything else takes a MANAGER (or an ADMIN, through the role hierarchy). The
 * first matching rule wins, so the narrow rules come before "/api/**". {@code *} is one path segment, the order id.
 */
@Configuration(proxyBeanMethods = false)
public class WebSecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http,
			@Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) throws Exception {
		JwtSecurity.configure(http, exceptionResolver)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(JwtSecurity.OPEN_PATHS).permitAll()
						.requestMatchers(HttpMethod.GET, "/api/**").hasRole(UserType.STAFF.name())
						.requestMatchers(HttpMethod.POST, "/api/customers").hasRole(UserType.STAFF.name())
						.requestMatchers(HttpMethod.PUT, "/api/customers/*").hasRole(UserType.STAFF.name())
						.requestMatchers(HttpMethod.POST, "/api/sales-orders").hasRole(UserType.STAFF.name())
						.requestMatchers(HttpMethod.POST, "/api/sales-orders/*/pay", "/api/sales-orders/*/ship",
								"/api/sales-orders/*/complete").hasRole(UserType.STAFF.name())
						// Written out, though "/api/**" below would give the same: cancelling is a decision of its own
						.requestMatchers(HttpMethod.POST, "/api/sales-orders/*/cancel").hasRole(UserType.MANAGER.name())
						.requestMatchers("/api/**").hasRole(UserType.MANAGER.name())
						.anyRequest().authenticated());
		return http.build();
	}
}
