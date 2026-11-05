package com.stockline.inventory.config;

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
 * Who may do what with stock (docs/conventions.md section 11). The same starting rule as product-service: every
 * signed-in user reads, changing takes a MANAGER (or an ADMIN, through the role hierarchy). The endpoints come with
 * T4.2 to T4.4, which refine this where a role needs something else.
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
						.requestMatchers("/api/**").hasRole(UserType.MANAGER.name())
						.anyRequest().authenticated());
		return http.build();
	}
}
