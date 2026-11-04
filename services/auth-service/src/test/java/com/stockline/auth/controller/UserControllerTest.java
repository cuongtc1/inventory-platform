package com.stockline.auth.controller;

import com.stockline.auth.config.SecurityConfig;
import com.stockline.auth.config.WebSecurityConfig;
import com.stockline.auth.constant.MessageKeys;
import com.stockline.auth.constant.UserStatus;
import com.stockline.auth.dto.CreateUserRequest;
import com.stockline.auth.dto.UpdateUserRequest;
import com.stockline.auth.dto.UserFilter;
import com.stockline.auth.dto.UserResponse;
import com.stockline.auth.service.UserService;
import com.stockline.common.constant.UserType;
import com.stockline.common.dto.PageResponse;
import com.stockline.common.exception.DuplicateResourceException;
import com.stockline.common.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static com.stockline.auth.TestTokens.as;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer with the real security rules; the service is a Mockito mock. Most important here: only ADMIN gets in.
 */
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, WebSecurityConfig.class})
class UserControllerTest {

	private static final Instant NOW = Instant.parse("2026-11-03T08:00:00Z");
	private static final UserResponse LINH = new UserResponse(7L, "linh@corp.com", "Linh Nguyen", UserType.MANAGER,
			UserStatus.ACTIVE, NOW, "admin@stockline.local", NOW, "admin@stockline.local");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@Test
	void list_asAdmin_returnsPageWithFiltersAndDefaultSort() throws Exception {
		// given
		when(userService.list(any(), any())).thenReturn(new PageResponse<>(List.of(LINH), 0, 20, 1, 1));

		// when / then
		mockMvc.perform(get("/api/users").with(as(1L, UserType.ADMIN))
						.param("search", "linh").param("userType", "MANAGER").param("status", "ACTIVE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].email").value("linh@corp.com"))
				.andExpect(jsonPath("$.content[0].createdBy").value("admin@stockline.local"))
				.andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
				.andExpect(jsonPath("$.totalElements").value(1));
		verify(userService).list(eq(new UserFilter("linh", UserType.MANAGER, UserStatus.ACTIVE)),
				argThat((Pageable pageable) -> pageable.getPageSize() == 20 && pageable.getSort().equals(Sort.by("fullName"))));
	}

	@Test
	void list_asManager_returns403ProblemDetail() throws Exception {
		mockMvc.perform(get("/api/users").with(as(7L, UserType.MANAGER)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"))
				.andExpect(jsonPath("$.detail").value("You don't have permission to do this"))
				.andExpect(jsonPath("$.instance").value("/api/users"));
		verifyNoInteractions(userService);
	}

	@Test
	void anyUsersEndpoint_asStaff_returns403() throws Exception {
		mockMvc.perform(post("/api/users/3/deactivate").with(as(8L, UserType.STAFF)))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/users/3").with(as(8L, UserType.STAFF)))
				.andExpect(status().isForbidden());
		verifyNoInteractions(userService);
	}

	@Test
	void list_withoutToken_returns401() throws Exception {
		mockMvc.perform(get("/api/users"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
		verifyNoInteractions(userService);
	}

	@Test
	void list_unknownUserType_returns400() throws Exception {
		mockMvc.perform(get("/api/users").with(as(1L, UserType.ADMIN)).param("userType", "BOSS"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	@Test
	void create_asAdmin_returns201WithLocation() throws Exception {
		// given
		CreateUserRequest request = new CreateUserRequest("linh@corp.com", "Linh Nguyen", UserType.MANAGER, "first-pass-1");
		when(userService.create(request)).thenReturn(LINH);

		// when / then
		mockMvc.perform(post("/api/users").with(as(1L, UserType.ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "linh@corp.com", "fullName": "Linh Nguyen", "userType": "MANAGER",
								  "password": "first-pass-1" }
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/users/7"))
				.andExpect(jsonPath("$.id").value(7));
	}

	@Test
	void create_invalidFields_returns400WithEveryField() throws Exception {
		mockMvc.perform(post("/api/users").with(as(1L, UserType.ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "not-an-email", "fullName": " ", "password": "short" }
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field", hasItems("email", "fullName", "userType", "password")));
		verifyNoInteractions(userService);
	}

	@Test
	void create_duplicateEmail_returns409() throws Exception {
		// given
		when(userService.create(any()))
				.thenThrow(new DuplicateResourceException(MessageKeys.USER_EMAIL_DUPLICATE, "linh@corp.com"));

		// when / then
		mockMvc.perform(post("/api/users").with(as(1L, UserType.ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "linh@corp.com", "fullName": "Linh", "userType": "STAFF", "password": "first-pass-1" }
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE"))
				.andExpect(jsonPath("$.detail").value("A user with email 'linh@corp.com' already exists"));
	}

	@Test
	void update_passesTheAdminFromTheToken() throws Exception {
		// given
		UpdateUserRequest request = new UpdateUserRequest("linh@corp.com", "Linh N.", UserType.STAFF);
		when(userService.update(1L, 7L, request)).thenReturn(LINH);

		// when / then
		mockMvc.perform(put("/api/users/7").with(as(1L, UserType.ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "linh@corp.com", "fullName": "Linh N.", "userType": "STAFF" }
								"""))
				.andExpect(status().isOk());
		verify(userService).update(1L, 7L, request);
	}

	@Test
	void deactivate_self_returns400() throws Exception {
		// given
		when(userService.deactivate(1L, 1L)).thenThrow(new InvalidRequestException(MessageKeys.USER_DEACTIVATE_SELF));

		// when / then
		mockMvc.perform(post("/api/users/1/deactivate").with(as(1L, UserType.ADMIN)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("You can't deactivate your own account"));
	}

	@Test
	void activate_asAdmin_returns200() throws Exception {
		// given
		when(userService.activate(7L)).thenReturn(LINH);

		// when / then
		mockMvc.perform(post("/api/users/7/activate").with(as(1L, UserType.ADMIN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ACTIVE"));
	}
}
