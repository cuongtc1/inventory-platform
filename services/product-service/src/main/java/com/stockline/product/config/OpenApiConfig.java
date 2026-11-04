package com.stockline.product.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Adds an "Authorize" button to Swagger UI: log in on auth-service (POST /api/auth/login), paste the
 * {@code accessToken} there, and Swagger UI sends it as {@code Authorization: Bearer ...} with every request.
 */
@Configuration(proxyBeanMethods = false)
@SecurityScheme(name = OpenApiConfig.BEARER, type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@SecurityRequirement(name = OpenApiConfig.BEARER)
public class OpenApiConfig {

	static final String BEARER = "bearerAuth";
}
