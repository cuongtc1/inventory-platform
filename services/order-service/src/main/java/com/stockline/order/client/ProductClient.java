package com.stockline.order.client;

import com.stockline.common.exception.ResourceNotFoundException;
import com.stockline.order.dto.ProductSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Reads products from product-service's REST API; this service never touches {@code product_db}. A service that
 * can't be reached becomes 503 in {@code GlobalExceptionHandler}.
 */
@Component
public class ProductClient {

	private final RestClient restClient;

	public ProductClient(RestClient.Builder builder, @Value("${stockline.services.product-url}") String productUrl) {
		this.restClient = builder.baseUrl(productUrl).requestInterceptor(new ForwardUserToken()).build();
	}

	public ProductSnapshot getProduct(Long id) {
		return restClient.get()
				.uri("/api/products/{id}", id)
				.retrieve()
				.onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND), (request, response) -> {
					throw new ResourceNotFoundException("Product", id);
				})
				.body(ProductSnapshot.class);
	}
}
