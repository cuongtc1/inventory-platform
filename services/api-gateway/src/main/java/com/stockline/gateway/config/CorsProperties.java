package com.stockline.gateway.config;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * {@code stockline.cors.*} from application.yml.
 *
 * @param allowedOrigins the web addresses whose pages may call this API from a browser; env
 *                       {@code CORS_ALLOWED_ORIGINS}, comma-separated
 */
@Validated
@ConfigurationProperties("stockline.cors")
public record CorsProperties(
		@NotEmpty List<String> allowedOrigins
) {
}
