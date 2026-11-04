package com.stockline.gateway;

import com.stockline.common.constant.JwtClaimNames;
import com.stockline.common.security.JwtSecurity;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A real gateway on a random port between a real HTTP client and fake services: small servers from the JDK
 * ({@code com.sun.net.httpserver}) that answer every request and remember what they received. So routing, the
 * JWT check, CORS and the error answers are tested the way a browser meets them. Tokens are really signed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayIntegrationTest {

	private static final String SECRET = "gateway-test-secret-at-least-32-bytes-long";
	private static final String ISSUER = "stockline-auth";
	private static final String FRONTEND = "http://localhost:5173";

	private static final FakeService AUTH_SERVICE = new FakeService("auth");
	private static final FakeService PRODUCT_SERVICE = new FakeService("product");
	private static final FakeService INVENTORY_SERVICE = new FakeService("inventory");
	private static final FakeService ORDER_SERVICE = new FakeService("order");
	private static final FakeService SLOW_SERVICE = new FakeService("slow", 3000);

	private static final HttpClient CLIENT = HttpClient.newHttpClient();

	@LocalServerPort
	private int port;

	/** Runs before Spring starts: points the routes in application.yml at the fake services. */
	@DynamicPropertySource
	static void gatewaySettings(DynamicPropertyRegistry registry) {
		registry.add("JWT_SECRET", () -> SECRET);
		registry.add("GATEWAY_READ_TIMEOUT", () -> "1s");
		registry.add("AUTH_SERVICE_URL", AUTH_SERVICE::url);
		registry.add("PRODUCT_SERVICE_URL", PRODUCT_SERVICE::url);
		registry.add("INVENTORY_SERVICE_URL", INVENTORY_SERVICE::url);
		registry.add("ORDER_SERVICE_URL", ORDER_SERVICE::url);
		registry.add("NOTIFICATION_SERVICE_URL", SLOW_SERVICE::url);
	}

	@BeforeEach
	void forgetRequests() {
		for (FakeService service : List.of(AUTH_SERVICE, PRODUCT_SERVICE, INVENTORY_SERVICE, ORDER_SERVICE)) {
			service.requests.clear();
		}
	}

	@AfterAll
	static void stopServices() {
		for (FakeService service : List.of(AUTH_SERVICE, PRODUCT_SERVICE, INVENTORY_SERVICE, ORDER_SERVICE, SLOW_SERVICE)) {
			service.server.stop(0);
		}
	}

	@Test
	void login_withoutToken_isForwardedWithItsBody() throws Exception {
		// when
		HttpResponse<String> response = send(request("/api/auth/login")
				.header(HttpHeaders.CONTENT_TYPE, "application/json")
				.POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"a@b.c\"}")));

		// then
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("\"service\":\"auth\"");
		assertThat(AUTH_SERVICE.requests).singleElement().satisfies(received -> {
			assertThat(received.method()).isEqualTo("POST");
			assertThat(received.pathAndQuery()).isEqualTo("/api/auth/login");
			assertThat(received.body()).isEqualTo("{\"email\":\"a@b.c\"}");
		});
	}

	@Test
	void validToken_isForwardedUnchangedToTheRightService() throws Exception {
		// given
		String token = token(SECRET, Instant.now().plusSeconds(600));

		// when
		HttpResponse<String> response = send(request("/api/products/7?fields=name&sort=sku,asc")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));

		// then: same path and query; the service gets the token too
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("\"service\":\"product\"");
		assertThat(PRODUCT_SERVICE.requests).singleElement().satisfies(received -> {
			assertThat(received.pathAndQuery()).isEqualTo("/api/products/7?fields=name&sort=sku,asc");
			assertThat(received.authorization()).isEqualTo("Bearer " + token);
		});
	}

	@Test
	void everyResource_goesToItsService() throws Exception {
		assertRoutedTo("/api/users", "auth");
		assertRoutedTo("/api/auth/me", "auth");
		assertRoutedTo("/api/categories", "product");
		assertRoutedTo("/api/suppliers/3", "product");
		assertRoutedTo("/api/warehouses", "inventory");
		assertRoutedTo("/api/stock-levels", "inventory");
		assertRoutedTo("/api/stock-movements", "inventory");
		assertRoutedTo("/api/customers", "order");
		assertRoutedTo("/api/sales-orders/5", "order");
		assertRoutedTo("/api/purchase-orders", "order");
	}

	@Test
	void serviceErrors_passThroughUnchanged() throws Exception {
		// when: the fake service answers this path with 409
		HttpResponse<String> response = send(request("/api/products/conflict")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(SECRET, Instant.now().plusSeconds(600))));

		// then
		assertThat(response.statusCode()).isEqualTo(409);
		assertThat(response.body()).contains("\"service\":\"product\"");
	}

	@Test
	void noToken_is401InOurFormat_andNeverReachesAService() throws Exception {
		// when
		HttpResponse<String> response = send(request("/api/products"));

		// then
		assertThat(response.statusCode()).isEqualTo(401);
		assertThat(response.body())
				.contains("\"code\":\"UNAUTHORIZED\"")
				.contains("\"instance\":\"/api/products\"");
		assertThat(PRODUCT_SERVICE.requests).isEmpty();
	}

	@Test
	void tokenSignedWithAnotherSecret_is401() throws Exception {
		// when
		HttpResponse<String> response = send(request("/api/products").header(HttpHeaders.AUTHORIZATION,
				"Bearer " + token("another-secret-that-is-at-least-32-bytes", Instant.now().plusSeconds(600))));

		// then
		assertThat(response.statusCode()).isEqualTo(401);
		assertThat(response.body()).contains("\"code\":\"UNAUTHORIZED\"");
		assertThat(PRODUCT_SERVICE.requests).isEmpty();
	}

	@Test
	void expiredToken_is401() throws Exception {
		// when: expired more than the validator's 60 seconds of tolerance ago
		HttpResponse<String> response = send(request("/api/products").header(HttpHeaders.AUTHORIZATION,
				"Bearer " + token(SECRET, Instant.now().minusSeconds(120))));

		// then
		assertThat(response.statusCode()).isEqualTo(401);
		assertThat(PRODUCT_SERVICE.requests).isEmpty();
	}

	@Test
	void unknownPath_withToken_is404InOurFormat() throws Exception {
		// when
		HttpResponse<String> response = send(request("/api/nothing-here")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(SECRET, Instant.now().plusSeconds(600))));

		// then
		assertThat(response.statusCode()).isEqualTo(404);
		assertThat(response.body()).contains("\"code\":\"NOT_FOUND\"");
	}

	@Test
	void serviceTooSlow_is503InOurFormat() throws Exception {
		// when
		HttpResponse<String> response = send(request("/api/notifications")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(SECRET, Instant.now().plusSeconds(600))));

		// then
		assertThat(response.statusCode()).isEqualTo(503);
		assertThat(response.body()).contains("\"code\":\"SERVICE_UNAVAILABLE\"");
	}

	@Test
	void preflightFromTheFrontend_isAllowedWithoutToken() throws Exception {
		// when: what a browser sends before a cross-origin request with an Authorization header
		HttpResponse<String> response = send(request("/api/products")
				.method("OPTIONS", HttpRequest.BodyPublishers.noBody())
				.header(HttpHeaders.ORIGIN, FRONTEND)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"));

		// then
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(header(response, HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo(FRONTEND);
		assertThat(header(response, HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).contains("PUT");
		assertThat(header(response, HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS).toLowerCase()).contains("authorization");
		assertThat(PRODUCT_SERVICE.requests).isEmpty();
	}

	@Test
	void preflightFromAnotherOrigin_isRefused() throws Exception {
		// when
		HttpResponse<String> response = send(request("/api/products")
				.method("OPTIONS", HttpRequest.BodyPublishers.noBody())
				.header(HttpHeaders.ORIGIN, "http://evil.example")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"));

		// then
		assertThat(response.statusCode()).isEqualTo(403);
		assertThat(response.headers().firstValue(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEmpty();
	}

	@Test
	void actualRequestFromTheFrontend_getsTheCorsHeaderOnce() throws Exception {
		// when
		HttpResponse<String> response = send(request("/api/products")
				.header(HttpHeaders.ORIGIN, FRONTEND)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(SECRET, Instant.now().plusSeconds(600))));

		// then: twice would make the browser reject the response
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.headers().allValues(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).containsExactly(FRONTEND);
	}

	@Test
	void actuatorHealth_isOpen() throws Exception {
		assertThat(send(request("/actuator/health")).statusCode()).isEqualTo(200);
	}

	private void assertRoutedTo(String path, String service) throws Exception {
		HttpResponse<String> response = send(request(path)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(SECRET, Instant.now().plusSeconds(600))));
		assertThat(response.statusCode()).as(path).isEqualTo(200);
		assertThat(response.body()).as(path).contains("\"service\":\"" + service + "\"");
	}

	private HttpRequest.Builder request(String path) {
		return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
	}

	private static HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
		return CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
	}

	private static String header(HttpResponse<?> response, String name) {
		return response.headers().firstValue(name).orElse("");
	}

	private static String token(String secret, Instant expiresAt) {
		JwtEncoder encoder = NimbusJwtEncoder.withSecretKey(JwtSecurity.secretKey(secret))
				.algorithm(MacAlgorithm.HS256).build();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(ISSUER)
				.subject("1")
				.issuedAt(expiresAt.minusSeconds(3600))
				.expiresAt(expiresAt)
				.claim(JwtClaimNames.ROLE, "STAFF")
				.build();
		return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
				.getTokenValue();
	}

	/** What a fake service received. */
	private record Received(String method, String pathAndQuery, String authorization, String body) {
	}

	/** A tiny HTTP server on a free port: answers {"service":"name"}, 409 for paths ending in /conflict. */
	private static final class FakeService {

		private final HttpServer server;
		private final List<Received> requests = new CopyOnWriteArrayList<>();

		FakeService(String name) {
			this(name, 0);
		}

		FakeService(String name, long delayMillis) {
			try {
				server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
			server.createContext("/", exchange -> handle(exchange, name, delayMillis));
			server.start();
		}

		String url() {
			return "http://localhost:" + server.getAddress().getPort();
		}

		private void handle(HttpExchange exchange, String name, long delayMillis) throws IOException {
			String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			// getPath() and getQuery() are decoded, like the values a Spring service sees ("," may arrive as "%2C")
			URI uri = exchange.getRequestURI();
			String pathAndQuery = uri.getQuery() == null ? uri.getPath() : uri.getPath() + "?" + uri.getQuery();
			requests.add(new Received(exchange.getRequestMethod(), pathAndQuery,
					exchange.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION), body));
			pause(delayMillis);
			byte[] answer = ("{\"service\":\"" + name + "\"}").getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
			int status = exchange.getRequestURI().getPath().endsWith("/conflict") ? 409 : 200;
			exchange.sendResponseHeaders(status, answer.length);
			exchange.getResponseBody().write(answer);
			exchange.close();
		}

		private static void pause(long millis) {
			try {
				Thread.sleep(millis);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}
}
