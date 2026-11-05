package com.stockline.inventory;

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
 * The rules of WebSecurityConfig with the real filters. POST without a body: a request the rules let through
 * reaches the controller and gets 400; one they stop gets 401 or 403.
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
		mockMvc.perform(get("/api/warehouses"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void staff_readsButDoesntChange() throws Exception {
		mockMvc.perform(get("/api/warehouses").with(as(UserType.STAFF))).andExpect(status().isOk());
		mockMvc.perform(post("/api/warehouses").with(as(UserType.STAFF)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
		mockMvc.perform(get("/api/stock-levels").with(as(UserType.STAFF))).andExpect(status().isOk());
		mockMvc.perform(post("/api/stock-levels/adjust").with(as(UserType.STAFF))).andExpect(status().isForbidden());
	}

	@Test
	void managerAndAdmin_getThrough() throws Exception {
		mockMvc.perform(post("/api/warehouses").with(as(UserType.MANAGER))).andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/warehouses").with(as(UserType.ADMIN))).andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/stock-levels/adjust").with(as(UserType.MANAGER))).andExpect(status().isBadRequest());
	}

	/** A request with an already "checked" token; the authority is set the way our role converter does. */
	private static RequestPostProcessor as(UserType role) {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}
}
