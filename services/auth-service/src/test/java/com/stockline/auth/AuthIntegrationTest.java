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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The whole application with a real MySQL (Testcontainers): Flyway creates {@code users}, the initializer creates
 * the admin from the {@code stockline.admin.*} properties below, and every request goes through the real security
 * filters with real signed tokens. Nothing is mocked.
 *
 * <p>Tests don't roll back, so each one uses its own users.
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

	@Autowired
	private JwtEncoder jwtEncoder;

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

	@Test
	void userManagement_rolesAndAccountLifecycle() throws Exception {
		String adminToken = login("first.admin@stockline.test", "it-admin-pass");

		// No token or a broken one: 401 in the usual error format
		mockMvc.perform(get("/api/users"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
		mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi"))
				.andExpect(status().isUnauthorized());

		// The ADMIN creates a STAFF user; the audit columns name the ADMIN
		String created = mockMvc.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "IT.Staff@stockline.test", "fullName": "IT Staff", "userType": "STAFF",
								  "password": "staff-pass-1" }
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value("it.staff@stockline.test"))
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andExpect(jsonPath("$.createdBy").value("first.admin@stockline.test"))
				.andReturn().getResponse().getContentAsString();
		Integer staffId = JsonPath.read(created, "$.id");

		// The STAFF user logs in, sees their own account, but not the user list
		String staffToken = login("it.staff@stockline.test", "staff-pass-1");
		mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(staffToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(staffId))
				.andExpect(jsonPath("$.userType").value("STAFF"));
		mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(staffToken)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));

		// Changing the password: the current one must be right; afterwards only the new one works
		changePassword(staffToken, "wrong-pass", "staff-pass-2").andExpect(status().isBadRequest());
		changePassword(staffToken, "staff-pass-1", "staff-pass-2").andExpect(status().isNoContent());
		loginRequest("it.staff@stockline.test", "staff-pass-1").andExpect(status().isUnauthorized());
		staffToken = login("it.staff@stockline.test", "staff-pass-2");

		// The ADMIN promotes them; updated_by names the ADMIN
		mockMvc.perform(put("/api/users/{id}", staffId).header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "it.staff@stockline.test", "fullName": "IT Staff", "userType": "MANAGER" }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userType").value("MANAGER"))
				.andExpect(jsonPath("$.updatedBy").value("first.admin@stockline.test"));

		// Deactivated: no new login, and /me with the old (still unexpired) token is refused too
		mockMvc.perform(post("/api/users/{id}/deactivate", staffId).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("INACTIVE"));
		loginRequest("it.staff@stockline.test", "staff-pass-2").andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(staffToken)))
				.andExpect(status().isUnauthorized());

		// Activated again: the password still works
		mockMvc.perform(post("/api/users/{id}/activate", staffId).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
				.andExpect(status().isOk());
		login("it.staff@stockline.test", "staff-pass-2");

		// The list finds them by part of the name, with the role filter
		mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
						.param("search", "it staff").param("userType", "MANAGER"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].id").value(staffId));
	}

	@Test
	void admin_cannotLockThemselvesOut() throws Exception {
		String adminToken = login("first.admin@stockline.test", "it-admin-pass");
		String me = mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
				.andReturn().getResponse().getContentAsString();
		Integer adminId = JsonPath.read(me, "$.id");

		mockMvc.perform(post("/api/users/{id}/deactivate", adminId).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("You can't deactivate your own account"));
		mockMvc.perform(put("/api/users/{id}", adminId).header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "first.admin@stockline.test", "fullName": "First Admin", "userType": "STAFF" }
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("You can't take the ADMIN role away from your own account"));
	}

	@Test
	void expiredToken_isRejected() throws Exception {
		// given: a correctly signed ADMIN token that expired a minute ago
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(jwtProperties.issuer())
				.subject("1")
				.issuedAt(now.minusSeconds(3600))
				.expiresAt(now.minusSeconds(60))
				.claim(JwtClaimNames.ROLE, "ADMIN")
				.build();
		String expired = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
				.getTokenValue();

		// when / then
		mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(expired)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	private String login(String email, String password) throws Exception {
		String body = loginRequest(email, password).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.accessToken");
	}

	private ResultActions loginRequest(String email, String password) throws Exception {
		return mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "email": "%s", "password": "%s" }
						""".formatted(email, password)));
	}

	private ResultActions changePassword(String token, String currentPassword, String newPassword) throws Exception {
		return mockMvc.perform(put("/api/auth/password").header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "currentPassword": "%s", "newPassword": "%s" }
						""".formatted(currentPassword, newPassword)));
	}

	private static String bearer(String token) {
		return "Bearer " + token;
	}
}
