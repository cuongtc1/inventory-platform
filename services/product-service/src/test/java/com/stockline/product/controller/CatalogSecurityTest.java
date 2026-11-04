package com.stockline.product.controller;

import com.stockline.common.constant.UserType;
import com.stockline.product.config.WebSecurityConfig;
import com.stockline.product.service.CategoryService;
import com.stockline.product.service.ProductService;
import com.stockline.product.service.SupplierService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The rule table of {@link WebSecurityConfig} with the real filters and the real role hierarchy; services are
 * mocks. A role that may pass reaches the controller, whatever it answers then (a mock's empty answer can even be
 * a 500), so "allowed" means: neither 401 nor 403.
 */
@WebMvcTest({CategoryController.class, SupplierController.class, ProductController.class})
@Import(WebSecurityConfig.class)
class CatalogSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CategoryService categoryService;

	@MockitoBean
	private SupplierService supplierService;

	@MockitoBean
	private ProductService productService;

	@ParameterizedTest(name = "{0} {1} as {2}")
	@CsvSource({
			"GET,    /api/products,     STAFF",
			"GET,    /api/products/1,   STAFF",
			"GET,    /api/categories,   STAFF",
			"GET,    /api/suppliers,    STAFF",
			"GET,    /api/products,     MANAGER",
			"GET,    /api/products,     ADMIN",
			"POST,   /api/products,     MANAGER",
			"PUT,    /api/products/1,   MANAGER",
			"DELETE, /api/products/1,   MANAGER",
			"POST,   /api/categories,   MANAGER",
			"DELETE, /api/suppliers/1,  MANAGER",
			// The hierarchy: an ADMIN can do what a MANAGER can
			"POST,   /api/products,     ADMIN",
			"PUT,    /api/categories/1, ADMIN",
			"DELETE, /api/products/1,   ADMIN"
	})
	void allowed(String method, String path, UserType role) throws Exception {
		int status = mockMvc.perform(request(HttpMethod.valueOf(method), path).with(as(role))).andReturn().getResponse().getStatus();

		assertThat(status).isNotIn(401, 403);
	}

	@ParameterizedTest(name = "{0} {1}")
	@CsvSource({
			"POST,   /api/products",
			"PUT,    /api/products/1",
			"DELETE, /api/products/1",
			"POST,   /api/categories",
			"PUT,    /api/categories/1",
			"DELETE, /api/categories/1",
			"POST,   /api/suppliers",
			"PUT,    /api/suppliers/1",
			"DELETE, /api/suppliers/1"
	})
	void staffCannotChangeTheCatalog(String method, String path) throws Exception {
		mockMvc.perform(body(request(HttpMethod.valueOf(method), path)).with(as(UserType.STAFF)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"))
				.andExpect(jsonPath("$.detail").value("You don't have permission to do this"));
	}

	@Test
	void noToken_is401InOurFormat() throws Exception {
		mockMvc.perform(get("/api/products"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
		mockMvc.perform(body(request(HttpMethod.POST, "/api/products")))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void garbageToken_is401() throws Exception {
		mockMvc.perform(get("/api/products").header("Authorization", "Bearer not.a.token"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void openPaths_needNoToken() throws Exception {
		// No handler for these in a controller slice, so 404: what matters is that security let them through
		int status = mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse().getStatus();

		assertThat(status).isNotIn(401, 403);
	}

	private static MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder request) {
		return request.contentType(MediaType.APPLICATION_JSON).content("{}");
	}

	/** A request with an already "checked" token; the authority is set the way our role converter does. */
	private static RequestPostProcessor as(UserType role) {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}
}
