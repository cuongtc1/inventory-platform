package com.stockline.product.exception;

import com.stockline.product.config.TimeConfig;
import com.stockline.product.controller.CategoryController;
import com.stockline.product.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * messages.properties claims that Spring MVC's own error texts (405, 415, 406) come from the
 * "problemDetail.&lt;exception class&gt;" keys. The real texts equal Spring's defaults, so this test loads
 * test-messages.properties first, with a different 405 text, and checks that the response uses it.
 */
@WebMvcTest(CategoryController.class)
@Import(TimeConfig.class)
// The first file that has a key wins
@TestPropertySource(properties = "spring.messages.basename=test-messages,messages")
class ProblemDetailMessagesTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CategoryService categoryService;

	@Test
	void methodNotAllowed_usesTextFromMessageSource() throws Exception {
		mockMvc.perform(patch("/api/categories/1"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.detail").value("TEST: PATCH is not allowed here"))
				.andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
	}
}
