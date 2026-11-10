package com.stockline.order.client;

import com.stockline.common.exception.ResourceNotFoundException;
import com.stockline.order.dto.WarehouseSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Reads warehouses from inventory-service's REST API; this service never touches {@code inventory_db}. A service
 * that can't be reached becomes 503 in {@code GlobalExceptionHandler}.
 */
@Component
public class WarehouseClient {

	private final RestClient restClient;

	public WarehouseClient(RestClient.Builder builder,
						   @Value("${stockline.services.inventory-url}") String inventoryUrl) {
		this.restClient = builder.baseUrl(inventoryUrl).requestInterceptor(new ForwardUserToken()).build();
	}

	public WarehouseSnapshot getWarehouse(Long id) {
		return restClient.get()
				.uri("/api/warehouses/{id}", id)
				.retrieve()
				.onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND), (request, response) -> {
					throw new ResourceNotFoundException("Warehouse", id);
				})
				.body(WarehouseSnapshot.class);
	}
}
