package com.stockline.order;

import com.stockline.common.constant.UserType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The rules of WebSecurityConfig with the real filters. There are no endpoints yet, so a request the rules let
 * through gets 404 from Spring MVC; one they stop gets 401 or 403.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SecurityRulesTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void healthAndApiDocs_areOpen() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
	}

	@Test
	void noToken_is401InOurFormat() throws Exception {
		mockMvc.perform(get("/api/customers"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void staff_getsPastReadsButNotChanges() throws Exception {
		mockMvc.perform(get("/api/customers").with(as(UserType.STAFF))).andExpect(status().isNotFound());
		mockMvc.perform(post("/api/customers").with(as(UserType.STAFF)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void managerAndAdmin_getThrough() throws Exception {
		mockMvc.perform(post("/api/customers").with(as(UserType.MANAGER))).andExpect(status().isNotFound());
		mockMvc.perform(post("/api/customers").with(as(UserType.ADMIN))).andExpect(status().isNotFound());
	}

	/** A request with a valid token of this role; the filter chain sees the same authority a real token gives. */
	private static RequestPostProcessor as(UserType userType) {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + userType.name()));
	}
}
