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
 * create sales orders, and add and edit customers, because they take the orders. Deleting and everything else takes a
 * MANAGER (or an ADMIN, through the role hierarchy). The first matching rule wins, so the narrow rules come before
 * "/api/**". Status changes of an order come with T5.4.
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
						.requestMatchers("/api/**").hasRole(UserType.MANAGER.name())
						.anyRequest().authenticated());
		return http.build();
	}
}
