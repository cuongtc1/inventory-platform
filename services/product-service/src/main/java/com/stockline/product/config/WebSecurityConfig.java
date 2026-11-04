package com.stockline.product.config;

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
 * Who may do what with the catalog (docs/conventions.md section 11). Every signed-in user can read it: staff need
 * products, and categories and suppliers for the list filters. Changing it takes a MANAGER; an ADMIN can too,
 * through the role hierarchy from stockline-common. The same token as at the gateway is checked here again, so
 * the rules hold even for a request that doesn't come through the gateway.
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
