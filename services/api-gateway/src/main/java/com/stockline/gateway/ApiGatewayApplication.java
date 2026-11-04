package com.stockline.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * api-gateway: the one entrance to Stockline. The routes are in application.yml, the JWT check and CORS in
 * {@code config/SecurityConfig}. There are no controllers and no database.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

}
