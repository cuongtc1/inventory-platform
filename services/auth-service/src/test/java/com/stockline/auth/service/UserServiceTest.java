package com.stockline.auth.service;

import com.stockline.auth.constant.MessageKeys;
import com.stockline.auth.constant.UserStatus;
import com.stockline.auth.dto.CreateUserRequest;
import com.stockline.auth.dto.UpdateUserRequest;
import com.stockline.auth.dto.UserResponse;
import com.stockline.auth.entity.User;
import com.stockline.auth.mapper.UserMapper;
import com.stockline.auth.repository.UserRepository;
import com.stockline.common.constant.UserType;
import com.stockline.common.exception.DuplicateResourceException;
import com.stockline.common.exception.InvalidRequestException;
import com.stockline.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static com.stockline.auth.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Plain unit test: the repository is a Mockito mock, the encoder a real BCrypt with the lowest cost.
 * ADMIN 1 is the one making the requests ({@code actorId}).
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(4);
	private static final Long ADMIN_ID = 1L;

	@Mock
	private UserRepository userRepository;

	private UserService userService;

	@BeforeEach
	void setUp() {
		userService = new UserService(userRepository, PASSWORD_ENCODER, new UserMapper());
	}

	@Test
	void create_newEmail_savesActiveUserWithNormalizedEmailAndHashedPassword() {
		// given
		when(userRepository.existsByEmail("linh@corp.com")).thenReturn(false);
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// when
		UserResponse created = userService.create(
				new CreateUserRequest(" Linh@Corp.com ", " Linh Nguyen ", UserType.MANAGER, "first-pass-1"));

		// then
		ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(saved.capture());
		assertThat(saved.getValue().getEmail()).isEqualTo("linh@corp.com");
		assertThat(saved.getValue().getFullName()).isEqualTo("Linh Nguyen");
		assertThat(saved.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(PASSWORD_ENCODER.matches("first-pass-1", saved.getValue().getPasswordHash())).isTrue();
		assertThat(created.userType()).isEqualTo(UserType.MANAGER);
	}

	@Test
	void create_emailTaken_throwsDuplicate() {
		// given
		when(userRepository.existsByEmail("linh@corp.com")).thenReturn(true);

		// when / then
		assertThatThrownBy(() -> userService.create(
				new CreateUserRequest("LINH@corp.com", "Linh", UserType.STAFF, "first-pass-1")))
				.isInstanceOfSatisfying(DuplicateResourceException.class, ex -> {
					assertThat(ex.getMessageKey()).isEqualTo(MessageKeys.USER_EMAIL_DUPLICATE);
					assertThat(ex.getMessageArgs()).containsExactly("linh@corp.com");
				});
		verify(userRepository, never()).save(any());
	}

	@Test
	void update_otherUser_changesEmailNameAndRole() {
		// given
		User linh = user(7L, "linh@corp.com", "hash", UserType.STAFF, UserStatus.ACTIVE);
		when(userRepository.findById(7L)).thenReturn(Optional.of(linh));
		when(userRepository.existsByEmailAndIdNot("linh.nguyen@corp.com", 7L)).thenReturn(false);

		// when
		UserResponse updated = userService.update(ADMIN_ID, 7L,
				new UpdateUserRequest("Linh.Nguyen@corp.com", "Linh Nguyen", UserType.MANAGER));

		// then
		assertThat(updated.email()).isEqualTo("linh.nguyen@corp.com");
		assertThat(updated.fullName()).isEqualTo("Linh Nguyen");
		assertThat(updated.userType()).isEqualTo(UserType.MANAGER);
		assertThat(linh.getPasswordHash()).isEqualTo("hash");
	}

	@Test
	void update_emailOfAnotherUser_throwsDuplicate() {
		// given
		when(userRepository.findById(7L)).thenReturn(Optional.of(
				user(7L, "linh@corp.com", "hash", UserType.STAFF, UserStatus.ACTIVE)));
		when(userRepository.existsByEmailAndIdNot("admin@stockline.local", 7L)).thenReturn(true);

		// when / then
		assertThatThrownBy(() -> userService.update(ADMIN_ID, 7L,
				new UpdateUserRequest("admin@stockline.local", "Linh", UserType.STAFF)))
				.isInstanceOf(DuplicateResourceException.class);
	}

	@Test
	void update_adminRemovesOwnAdminRole_throwsAndChangesNothing() {
		// given
		User admin = user(ADMIN_ID, "admin@stockline.local", "hash", UserType.ADMIN, UserStatus.ACTIVE);
		when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
		when(userRepository.existsByEmailAndIdNot("admin@stockline.local", ADMIN_ID)).thenReturn(false);

		// when / then
		assertThatThrownBy(() -> userService.update(ADMIN_ID, ADMIN_ID,
				new UpdateUserRequest("admin@stockline.local", "Admin", UserType.MANAGER)))
				.isInstanceOfSatisfying(InvalidRequestException.class,
						ex -> assertThat(ex.getMessageKey()).isEqualTo(MessageKeys.USER_ROLE_SELF));
		assertThat(admin.getUserType()).isEqualTo(UserType.ADMIN);
	}

	@Test
	void update_adminChangesOwnNameKeepingAdmin_isAllowed() {
		// given
		User admin = user(ADMIN_ID, "admin@stockline.local", "hash", UserType.ADMIN, UserStatus.ACTIVE);
		when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
		when(userRepository.existsByEmailAndIdNot("admin@stockline.local", ADMIN_ID)).thenReturn(false);

		// when
		UserResponse updated = userService.update(ADMIN_ID, ADMIN_ID,
				new UpdateUserRequest("admin@stockline.local", "The Admin", UserType.ADMIN));

		// then
		assertThat(updated.fullName()).isEqualTo("The Admin");
	}

	@Test
	void update_unknownId_throwsNotFound() {
		// given
		when(userRepository.findById(99L)).thenReturn(Optional.empty());

		// when / then
		assertThatThrownBy(() -> userService.update(ADMIN_ID, 99L,
				new UpdateUserRequest("x@corp.com", "X", UserType.STAFF)))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void deactivate_otherUser_setsInactive() {
		// given
		User linh = user(7L, "linh@corp.com", "hash", UserType.MANAGER, UserStatus.ACTIVE);
		when(userRepository.findById(7L)).thenReturn(Optional.of(linh));

		// when
		UserResponse result = userService.deactivate(ADMIN_ID, 7L);

		// then
		assertThat(result.status()).isEqualTo(UserStatus.INACTIVE);
		assertThat(linh.isActive()).isFalse();
	}

	@Test
	void deactivate_self_throwsWithoutLoadingAnything() {
		// when / then
		assertThatThrownBy(() -> userService.deactivate(ADMIN_ID, ADMIN_ID))
				.isInstanceOfSatisfying(InvalidRequestException.class,
						ex -> assertThat(ex.getMessageKey()).isEqualTo(MessageKeys.USER_DEACTIVATE_SELF));
		verify(userRepository, never()).findById(any());
	}

	@Test
	void activate_inactiveUser_setsActive() {
		// given
		User old = user(8L, "old@corp.com", "hash", UserType.STAFF, UserStatus.INACTIVE);
		when(userRepository.findById(8L)).thenReturn(Optional.of(old));

		// when
		UserResponse result = userService.activate(8L);

		// then
		assertThat(result.status()).isEqualTo(UserStatus.ACTIVE);
	}
}
