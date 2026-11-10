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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The rules of WebSecurityConfig with the real filters. POST and PUT without a body: a request the rules let through
 * reaches the controller and gets 400; one they stop gets 401 or 403. DELETE of an id that doesn't exist: through the
 * rules it gets 404.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SecurityRulesTest {

	private static final long UNKNOWN_ID = 999_999;

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
	void staff_readsAddsAndEditsCustomers() throws Exception {
		mockMvc.perform(get("/api/customers").with(as(UserType.STAFF))).andExpect(status().isOk());
		mockMvc.perform(post("/api/customers").with(as(UserType.STAFF))).andExpect(status().isBadRequest());
		mockMvc.perform(put("/api/customers/1").with(as(UserType.STAFF))).andExpect(status().isBadRequest());
	}

	@Test
	void staff_createsSalesOrders() throws Exception {
		mockMvc.perform(get("/api/sales-orders").with(as(UserType.STAFF))).andExpect(status().isOk());
		mockMvc.perform(post("/api/sales-orders").with(as(UserType.STAFF))).andExpect(status().isBadRequest());
	}

	@Test
	void staff_paysShipsAndCompletes_butDoesntCancel() throws Exception {
		// Through the rules, an order that doesn't exist is 404
		for (String verb : new String[] {"pay", "ship", "complete"}) {
			mockMvc.perform(post("/api/sales-orders/{id}/{verb}", UNKNOWN_ID, verb).with(as(UserType.STAFF)))
					.andExpect(status().isNotFound());
		}
		mockMvc.perform(post("/api/sales-orders/{id}/cancel", UNKNOWN_ID).with(as(UserType.STAFF)))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/sales-orders/{id}/cancel", UNKNOWN_ID).with(as(UserType.MANAGER)))
				.andExpect(status().isNotFound());
	}

	@Test
	void staff_doesntDeleteCustomers() throws Exception {
		mockMvc.perform(delete("/api/customers/{id}", UNKNOWN_ID).with(as(UserType.STAFF)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void managerAndAdmin_getThrough() throws Exception {
		mockMvc.perform(post("/api/customers").with(as(UserType.MANAGER))).andExpect(status().isBadRequest());
		mockMvc.perform(delete("/api/customers/{id}", UNKNOWN_ID).with(as(UserType.MANAGER)))
				.andExpect(status().isNotFound());
		mockMvc.perform(delete("/api/customers/{id}", UNKNOWN_ID).with(as(UserType.ADMIN)))
				.andExpect(status().isNotFound());
	}

	/** A request with a valid token of this role; the filter chain sees the same authority a real token gives. */
	private static RequestPostProcessor as(UserType userType) {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + userType.name()));
	}
}
