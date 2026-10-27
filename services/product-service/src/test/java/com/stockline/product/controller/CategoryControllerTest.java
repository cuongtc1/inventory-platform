package com.stockline.product.controller;

import com.stockline.common.dto.PageResponse;
import com.stockline.common.exception.DuplicateResourceException;
import com.stockline.common.exception.ResourceInUseException;
import com.stockline.common.exception.ResourceNotFoundException;
import com.stockline.product.constant.MessageKeys;
import com.stockline.product.dto.CategoryRequest;
import com.stockline.product.dto.CategoryResponse;
import com.stockline.product.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Starts only the web layer (this controller, JSON conversion, validation, GlobalExceptionHandler).
 * The service is a Mockito mock, so no database is needed. Checks URLs, status codes and JSON shape.
 */
@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

	private static final Instant NOW = Instant.parse("2026-10-23T08:00:00Z");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CategoryService categoryService;

	@Test
	void list_noParameters_usesDefaultPageAndReturnsPageJson() throws Exception {
		// given
		CategoryResponse cables = new CategoryResponse(2L, "Cables", 1L, "Electronics", 2, NOW, "system", NOW, "system");
		when(categoryService.list(isNull(), any(Pageable.class)))
				.thenReturn(new PageResponse<>(List.of(cables), 0, 20, 1, 1));

		// when / then
		mockMvc.perform(get("/api/categories"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].name").value("Cables"))
				.andExpect(jsonPath("$.content[0].parentName").value("Electronics"))
				.andExpect(jsonPath("$.content[0].productCount").value(2))
				.andExpect(jsonPath("$.content[0].createdAt").value("2026-10-23T08:00:00Z"))
				.andExpect(jsonPath("$.content[0].createdBy").value("system"))
				.andExpect(jsonPath("$.content[0].updatedBy").value("system"))
				.andExpect(jsonPath("$.totalElements").value(1));
		verify(categoryService).list(isNull(), argThat(pageable ->
				pageable.getPageNumber() == 0
						&& pageable.getPageSize() == 20
						&& pageable.getSort().equals(Sort.by("name"))));
	}

	@Test
	void list_sizeAboveMaximum_isLimitedTo100() throws Exception {
		// given
		when(categoryService.list(eq("cab"), any(Pageable.class)))
				.thenReturn(new PageResponse<>(List.of(), 0, 100, 0, 0));

		// when / then
		mockMvc.perform(get("/api/categories").param("search", "cab").param("size", "500"))
				.andExpect(status().isOk());
		verify(categoryService).list(eq("cab"), argThat(pageable -> pageable.getPageSize() == 100));
	}

	@Test
	void get_existingId_returns200WithCategory() throws Exception {
		// given
		when(categoryService.get(2L)).thenReturn(new CategoryResponse(2L, "Cables", 1L, "Electronics", 2, NOW, "system", NOW, "system"));

		// when / then
		mockMvc.perform(get("/api/categories/2"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id").value(2))
				.andExpect(jsonPath("$.name").value("Cables"))
				.andExpect(jsonPath("$.parentId").value(1))
				.andExpect(jsonPath("$.parentName").value("Electronics"))
				.andExpect(jsonPath("$.updatedAt").value("2026-10-23T08:00:00Z"));
	}

	@Test
	void update_validRequest_returns200WithUpdatedCategory() throws Exception {
		// given
		when(categoryService.update(2L, new CategoryRequest("USB Cables", null)))
				.thenReturn(new CategoryResponse(2L, "USB Cables", null, null, 0, NOW, "system", NOW, "system"));

		// when / then
		mockMvc.perform(put("/api/categories/2")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "USB Cables" }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("USB Cables"))
				.andExpect(jsonPath("$.parentId").isEmpty());
	}

	@Test
	void get_unknownId_returns404ProblemJson() throws Exception {
		// given
		when(categoryService.get(99L)).thenThrow(new ResourceNotFoundException("Category", 99L));

		// when / then
		mockMvc.perform(get("/api/categories/99"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("about:blank"))
				.andExpect(jsonPath("$.title").value("Not Found"))
				.andExpect(jsonPath("$.detail").value("Category with id 99 not found"))
				.andExpect(jsonPath("$.instance").value("/api/categories/99"))
				.andExpect(jsonPath("$.code").value("NOT_FOUND"))
				.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void create_validRequest_returns201WithLocation() throws Exception {
		// given
		when(categoryService.create(new CategoryRequest("Cables", 1L)))
				.thenReturn(new CategoryResponse(5L, "Cables", 1L, "Electronics", 0, NOW, "system", NOW, "system"));

		// when / then
		mockMvc.perform(post("/api/categories")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "Cables", "parentId": 1 }
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/categories/5"))
				.andExpect(jsonPath("$.id").value(5));
	}

	@Test
	void create_blankNameAndNegativeParent_returns400WithFieldErrors() throws Exception {
		// when / then
		mockMvc.perform(post("/api/categories")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "  ", "parentId": -1 }
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.instance").value("/api/categories"))
				.andExpect(jsonPath("$.errors.length()").value(2))
				.andExpect(jsonPath("$.errors[?(@.field == 'name')].message").value("must not be blank"))
				.andExpect(jsonPath("$.errors[?(@.field == 'parentId')].message").value("must be greater than 0"));
		verifyNoInteractions(categoryService);
	}

	@Test
	void update_duplicateName_returns409Duplicate() throws Exception {
		// given
		when(categoryService.update(2L, new CategoryRequest("Displays", null)))
				.thenThrow(new DuplicateResourceException(MessageKeys.CATEGORY_NAME_DUPLICATE, "Displays"));

		// when / then
		mockMvc.perform(put("/api/categories/2")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "Displays" }
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE"));
	}

	@Test
	void delete_existingCategory_returns204() throws Exception {
		// when / then
		mockMvc.perform(delete("/api/categories/2"))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));
		verify(categoryService).delete(2L);
	}

	@Test
	void delete_categoryInUse_returns409InUse() throws Exception {
		// given
		doThrow(new ResourceInUseException(MessageKeys.CATEGORY_DELETE_HAS_PRODUCTS, "Cables"))
				.when(categoryService).delete(2L);

		// when / then
		mockMvc.perform(delete("/api/categories/2"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("IN_USE"));
	}
}
