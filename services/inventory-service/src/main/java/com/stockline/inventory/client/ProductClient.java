package com.stockline.inventory.client;

import com.stockline.common.exception.ResourceNotFoundException;
import com.stockline.inventory.dto.ProductSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Reads products from product-service's REST API; this service never touches {@code product_db}.
 *
 * <p>The call carries the token of the user who made the request, because product-service checks tokens too. A
 * service that can't be reached becomes 503 in {@code GlobalExceptionHandler}.
 *
 * <p>{@code RestClient} (Spring 6.1) is the modern, fluent replacement for {@code RestTemplate}.
 */
@Component
public class ProductClient {

	private final RestClient restClient;

	public ProductClient(RestClient.Builder builder, @Value("${stockline.services.product-url}") String productUrl) {
		this.restClient = builder.baseUrl(productUrl).build();
	}

	public ProductSnapshot getProduct(Long id) {
		return restClient.get()
				.uri("/api/products/{id}", id)
				.headers(headers -> headers.setBearerAuth(currentToken()))
				.retrieve()
				.onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND), (request, response) -> {
					throw new ResourceNotFoundException("Product", id);
				})
				.body(ProductSnapshot.class);
	}

	private static String currentToken() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication instanceof JwtAuthenticationToken token) {
			return token.getToken().getTokenValue();
		}
		throw new IllegalStateException("A call to product-service needs a signed-in user");
	}
}
