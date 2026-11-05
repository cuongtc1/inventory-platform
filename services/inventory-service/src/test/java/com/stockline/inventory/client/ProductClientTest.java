package com.stockline.inventory.client;

import com.stockline.common.exception.ResourceNotFoundException;
import com.stockline.inventory.dto.ProductSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
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
 * {@code MockRestServiceServer} from spring-test answers the RestClient's calls without a network.
 */
class ProductClientTest {

	private MockRestServiceServer server;
	private ProductClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		client = new ProductClient(builder, "http://product-service");
		Jwt token = Jwt.withTokenValue("the-token").header("alg", "HS256").subject("1").build();
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(token));
	}

	@AfterEach
	void clearUser() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void getProduct_sendsTheUsersTokenAndIgnoresOtherFields() {
		// given
		server.expect(requestTo("http://product-service/api/products/12"))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer the-token"))
				.andRespond(withSuccess("""
						{ "id": 12, "sku": "A01", "name": "Wireless Mouse", "reorderLevel": 20,
						  "salePrice": 12.00, "status": "ACTIVE" }
						""", MediaType.APPLICATION_JSON));

		// when
		ProductSnapshot product = client.getProduct(12L);

		// then
		assertThat(product).isEqualTo(new ProductSnapshot(12L, "A01", "Wireless Mouse", 20));
		server.verify();
	}

	@Test
	void getProduct_unknownId_throwsNotFound() {
		// given
		server.expect(requestTo("http://product-service/api/products/99")).andRespond(withStatus(HttpStatus.NOT_FOUND));

		// when / then
		assertThatThrownBy(() -> client.getProduct(99L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void getProduct_withoutASignedInUser_failsClearly() {
		// given
		SecurityContextHolder.clearContext();

		// when / then
		assertThatThrownBy(() -> client.getProduct(12L)).isInstanceOf(IllegalStateException.class);
	}
}
