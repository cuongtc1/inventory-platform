package com.stockline.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * auth-service: users, passwords and login.
 *
 * <p>{@code @ConfigurationPropertiesScan} finds the {@code @ConfigurationProperties} records in this package
 * ({@code JwtProperties}, {@code AdminProperties}) and fills them from application.yml.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class AuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}

}
