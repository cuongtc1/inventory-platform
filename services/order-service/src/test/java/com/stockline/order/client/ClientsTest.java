package com.stockline.order.client;

import com.stockline.common.exception.ResourceNotFoundException;
import com.stockline.order.dto.ProductSnapshot;
import com.stockline.order.dto.WarehouseSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * ProductClient and WarehouseClient against {@code MockRestServiceServer}, which answers without a network: the
 * user's token is forwarded, the fields are read, a 404 becomes our own NOT_FOUND.
 */
class ClientsTest {

	@BeforeEach
	void signIn() {
		Jwt token = Jwt.withTokenValue("the-token").header("alg", "HS256").subject("1").build();
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(token));
	}

	@AfterEach
	void signOut() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void product_isReadWithTheUsersToken() {
		// given
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		ProductClient client = new ProductClient(builder, "http://product-service");
		server.expect(requestTo("http://product-service/api/products/6"))
				.andExpect(header("Authorization", "Bearer the-token"))
				.andRespond(withSuccess("""
						{ "id": 6, "sku": "A01", "name": "Wireless Mouse", "salePrice": 12.00, "status": "ACTIVE",
						  "costPrice": 7.50, "categoryName": "Accessories" }
						""", MediaType.APPLICATION_JSON));

		// when
		ProductSnapshot product = client.getProduct(6L);

		// then
		assertThat(product.salePrice()).isEqualByComparingTo("12.00");
		assertThat(product.isActive()).isTrue();
		server.verify();
	}

	@Test
	void product_404_becomesOurNotFound() {
		// given
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		ProductClient client = new ProductClient(builder, "http://product-service");
		server.expect(requestTo("http://product-service/api/products/99")).andRespond(withStatus(HttpStatus.NOT_FOUND));

		// when / then
		// The text is looked up later from the message key; the arguments say what wasn't found
		assertThatThrownBy(() -> client.getProduct(99L))
				.isInstanceOfSatisfying(ResourceNotFoundException.class,
						ex -> assertThat(ex.getMessageArgs()).containsExactly("Product", 99L));
	}

	@Test
	void warehouse_isReadWithTheUsersToken() {
		// given
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		WarehouseClient client = new WarehouseClient(builder, "http://inventory-service");
		server.expect(requestTo("http://inventory-service/api/warehouses/2"))
				.andExpect(header("Authorization", "Bearer the-token"))
				.andRespond(withSuccess("""
						{ "id": 2, "code": "WH-01", "name": "Main Warehouse", "skuCount": 2 }
						""", MediaType.APPLICATION_JSON));

		// when
		WarehouseSnapshot warehouse = client.getWarehouse(2L);

		// then
		assertThat(warehouse.code()).isEqualTo("WH-01");
		server.verify();
	}

	@Test
	void warehouse_404_becomesOurNotFound() {
		// given
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		WarehouseClient client = new WarehouseClient(builder, "http://inventory-service");
		server.expect(requestTo("http://inventory-service/api/warehouses/9")).andRespond(withStatus(HttpStatus.NOT_FOUND));

		// when / then
		assertThatThrownBy(() -> client.getWarehouse(9L))
				.isInstanceOfSatisfying(ResourceNotFoundException.class,
						ex -> assertThat(ex.getMessageArgs()).containsExactly("Warehouse", 9L));
	}

	@Test
	void withoutASignedInUser_noCallIsMade() {
		// given
		SecurityContextHolder.clearContext();
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		ProductClient client = new ProductClient(builder, "http://product-service");

		// when / then
		assertThatThrownBy(() -> client.getProduct(6L)).isInstanceOf(IllegalStateException.class);
		server.verify();
	}
}
