package com.stockline.auth;

import com.jayway.jsonpath.JsonPath;
import com.stockline.auth.config.JwtProperties;
import com.stockline.auth.config.SecurityConfig;
import com.stockline.common.constant.JwtClaimNames;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The whole application with a real MySQL (Testcontainers): Flyway creates {@code users}, the initializer creates
 * the admin from the {@code stockline.admin.*} properties below, and login is called over HTTP. Nothing is mocked.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
		"stockline.admin.email=First.Admin@Stockline.test",
		"stockline.admin.password=it-admin-pass",
		"stockline.admin.full-name=First Admin"
})
class AuthIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtProperties jwtProperties;

	@Test
	void login_seededAdmin_returnsTokenWithIdAndRole() throws Exception {
		// when: the email in other capitals and with spaces
		String body = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": " FIRST.ADMIN@stockline.test ", "password": "it-admin-pass" }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.user.email").value("first.admin@stockline.test"))
				.andExpect(jsonPath("$.user.fullName").value("First Admin"))
				.andExpect(jsonPath("$.user.userType").value("ADMIN"))
				.andReturn().getResponse().getContentAsString();

		// then: the token is signed with JWT_SECRET and carries the user's id and role
		String token = JsonPath.read(body, "$.accessToken");
		Integer userId = JsonPath.read(body, "$.user.id");
		Jwt jwt = NimbusJwtDecoder.withSecretKey(SecurityConfig.secretKey(jwtProperties))
				.macAlgorithm(MacAlgorithm.HS256).build()
				.decode(token);
		assertThat(jwt.getSubject()).isEqualTo(String.valueOf(userId));
		assertThat(jwt.getClaimAsString(JwtClaimNames.ROLE)).isEqualTo("ADMIN");
		assertThat(jwt.getClaimAsString(JwtClaimNames.EMAIL)).isEqualTo("first.admin@stockline.test");
		assertThat(jwt.getExpiresAt()).isEqualTo(jwt.getIssuedAt().plus(jwtProperties.expiration()));
	}

	@Test
	void login_wrongPasswordOrUnknownEmail_return401WithTheSameText() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "first.admin@stockline.test", "password": "wrong" }
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
				.andExpect(jsonPath("$.detail").value("Wrong email or password"));

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "nobody@stockline.test", "password": "it-admin-pass" }
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("Wrong email or password"));
	}
}
