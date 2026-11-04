package com.stockline.product;

import com.jayway.jsonpath.JsonPath;
import com.stockline.common.constant.JwtClaimNames;
import com.stockline.common.constant.UserType;
import com.stockline.common.security.JwtCheckProperties;
import com.stockline.common.security.JwtSecurity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The whole application with MySQL and the real security filters. Tokens are signed here the way auth-service
 * signs them, with the secret from application.yml, so the real {@code JwtDecoder} checks them.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SecurityIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtCheckProperties jwtProperties;

	@Test
	void rolesThroughTheWholeLifecycle() throws Exception {
		String staff = token(UserType.STAFF, Instant.now().plusSeconds(600));
		String manager = token(UserType.MANAGER, Instant.now().plusSeconds(600));
		String admin = token(UserType.ADMIN, Instant.now().plusSeconds(600));

		// A STAFF token reads but doesn't create
		mockMvc.perform(get("/api/categories").header(HttpHeaders.AUTHORIZATION, staff))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/categories").header(HttpHeaders.AUTHORIZATION, staff)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "SEC Staff" }
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));

		// A MANAGER creates; the audit columns name them
		String created = mockMvc.perform(post("/api/categories").header(HttpHeaders.AUTHORIZATION, manager)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "SEC Cables" }
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.createdBy").value("manager@stockline.test"))
				.andExpect(jsonPath("$.updatedBy").value("manager@stockline.test"))
				.andReturn().getResponse().getContentAsString();
		Integer id = JsonPath.read(created, "$.id");

		// An ADMIN can do what a MANAGER can (the hierarchy); updated_by follows, created_by stays
		mockMvc.perform(put("/api/categories/{id}", id).header(HttpHeaders.AUTHORIZATION, admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "SEC Cables 2" }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.createdBy").value("manager@stockline.test"))
				.andExpect(jsonPath("$.updatedBy").value("admin@stockline.test"));
		mockMvc.perform(delete("/api/categories/{id}", id).header(HttpHeaders.AUTHORIZATION, staff))
				.andExpect(status().isForbidden());
		mockMvc.perform(delete("/api/categories/{id}", id).header(HttpHeaders.AUTHORIZATION, admin))
				.andExpect(status().isNoContent());
	}

	@Test
	void missingOrExpiredToken_is401() throws Exception {
		mockMvc.perform(get("/api/products"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
		mockMvc.perform(get("/api/products").header(HttpHeaders.AUTHORIZATION,
						token(UserType.ADMIN, Instant.now().minus(Duration.ofMinutes(5)))))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void health_isOpen() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

	private String token(UserType role, Instant expiresAt) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(jwtProperties.issuer())
				.subject("1")
				.issuedAt(expiresAt.minus(Duration.ofHours(1)))
				.expiresAt(expiresAt)
				.claim(JwtClaimNames.ROLE, role.name())
				.claim(JwtClaimNames.EMAIL, role.name().toLowerCase() + "@stockline.test")
				.build();
		String token = NimbusJwtEncoder.withSecretKey(JwtSecurity.secretKey(jwtProperties.secret()))
				.algorithm(MacAlgorithm.HS256).build()
				.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
				.getTokenValue();
		return "Bearer " + token;
	}
}
