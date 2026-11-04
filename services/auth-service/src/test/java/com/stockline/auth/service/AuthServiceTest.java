package com.stockline.auth.service;

import com.stockline.auth.constant.MessageKeys;
import com.stockline.auth.constant.UserStatus;
import com.stockline.auth.dto.ChangePasswordRequest;
import com.stockline.auth.dto.LoginRequest;
import com.stockline.auth.dto.LoginResponse;
import com.stockline.auth.dto.UserResponse;
import com.stockline.auth.entity.User;
import com.stockline.auth.mapper.UserMapper;
import com.stockline.auth.repository.UserRepository;
import com.stockline.common.constant.CommonMessageKeys;
import com.stockline.common.constant.UserType;
import com.stockline.common.exception.InvalidRequestException;
import com.stockline.common.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Optional;

import static com.stockline.auth.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Plain unit test: no Spring, no database. The repository and the token service are Mockito mocks; the password
 * encoder is a real BCrypt with the lowest cost (4), so hashing takes microseconds instead of ~100 ms.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(4);
	private static final String PASSWORD = "secret-pass-1";
	private static final String PASSWORD_HASH = PASSWORD_ENCODER.encode(PASSWORD);
	private static final Instant EXPIRES_AT = Instant.parse("2026-11-03T16:00:00Z");

	@Mock
	private UserRepository userRepository;

	@Mock
	private TokenService tokenService;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(userRepository, PASSWORD_ENCODER, tokenService, new UserMapper());
	}

	@Test
	void login_correctPassword_returnsTokenAndUser() {
		// given
		User manager = user(7L, "linh@corp.com", PASSWORD_HASH, UserType.MANAGER, UserStatus.ACTIVE);
		when(userRepository.findByEmail("linh@corp.com")).thenReturn(Optional.of(manager));
		when(tokenService.createToken(manager)).thenReturn(jwt("header.claims.signature"));

		// when
		LoginResponse response = authService.login(new LoginRequest("linh@corp.com", PASSWORD));

		// then
		assertThat(response.accessToken()).isEqualTo("header.claims.signature");
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.expiresAt()).isEqualTo(EXPIRES_AT);
		assertThat(response.user()).isEqualTo(
				new LoginResponse.LoggedInUser(7L, "linh@corp.com", "Test manager", UserType.MANAGER));
	}

	@Test
	void login_emailWithSpacesAndCapitals_findsUserByNormalizedEmail() {
		// given
		User manager = user(7L, "linh@corp.com", PASSWORD_HASH, UserType.MANAGER, UserStatus.ACTIVE);
		when(userRepository.findByEmail("linh@corp.com")).thenReturn(Optional.of(manager));
		when(tokenService.createToken(manager)).thenReturn(jwt("token"));

		// when
		LoginResponse response = authService.login(new LoginRequest("  Linh@Corp.COM ", PASSWORD));

		// then
		assertThat(response.user().email()).isEqualTo("linh@corp.com");
	}

	@Test
	void login_wrongPassword_throwsUnauthorized() {
		// given
		when(userRepository.findByEmail("linh@corp.com")).thenReturn(Optional.of(
				user(7L, "linh@corp.com", PASSWORD_HASH, UserType.MANAGER, UserStatus.ACTIVE)));

		// when / then
		assertLoginFails(new LoginRequest("linh@corp.com", "wrong-pass"));
	}

	@Test
	void login_unknownEmail_throwsTheSameUnauthorized() {
		// given
		when(userRepository.findByEmail("nobody@corp.com")).thenReturn(Optional.empty());

		// when / then
		assertLoginFails(new LoginRequest("nobody@corp.com", PASSWORD));
	}

	@Test
	void login_deactivatedUserWithCorrectPassword_throwsTheSameUnauthorized() {
		// given
		when(userRepository.findByEmail("old@corp.com")).thenReturn(Optional.of(
				user(8L, "old@corp.com", PASSWORD_HASH, UserType.STAFF, UserStatus.INACTIVE)));

		// when / then
		assertLoginFails(new LoginRequest("old@corp.com", PASSWORD));
	}

	@Test
	void login_passwordLongerThan72Bytes_throwsUnauthorizedNotAServerError() {
		// given: 72 characters pass the request's @Size, but "ằ" is 3 bytes in UTF-8, so this is 216 bytes,
		// more than BCrypt reads
		when(userRepository.findByEmail("linh@corp.com")).thenReturn(Optional.of(
				user(7L, "linh@corp.com", PASSWORD_HASH, UserType.MANAGER, UserStatus.ACTIVE)));

		// when / then
		assertLoginFails(new LoginRequest("linh@corp.com", "ằ".repeat(72)));
	}

	@Test
	void me_activeUser_returnsUserWithoutHash() {
		// given
		when(userRepository.findById(7L)).thenReturn(Optional.of(
				user(7L, "linh@corp.com", PASSWORD_HASH, UserType.MANAGER, UserStatus.ACTIVE)));

		// when
		UserResponse me = authService.me(7L);

		// then
		assertThat(me.email()).isEqualTo("linh@corp.com");
		assertThat(me.userType()).isEqualTo(UserType.MANAGER);
	}

	@Test
	void me_userDeactivatedAfterLogin_throwsUnauthorized() {
		// given: the token is still valid, but the account was deactivated meanwhile
		when(userRepository.findById(8L)).thenReturn(Optional.of(
				user(8L, "old@corp.com", PASSWORD_HASH, UserType.STAFF, UserStatus.INACTIVE)));

		// when / then
		assertThatThrownBy(() -> authService.me(8L))
				.isInstanceOfSatisfying(UnauthorizedException.class,
						ex -> assertThat(ex.getMessageKey()).isEqualTo(CommonMessageKeys.AUTH_UNAUTHORIZED));
	}

	@Test
	void changePassword_correctCurrentPassword_storesNewHash() {
		// given
		User linh = user(7L, "linh@corp.com", PASSWORD_HASH, UserType.MANAGER, UserStatus.ACTIVE);
		when(userRepository.findById(7L)).thenReturn(Optional.of(linh));

		// when
		authService.changePassword(7L, new ChangePasswordRequest(PASSWORD, "brand-new-pass"));

		// then: the entity is changed; the transaction writes it (no save() call needed)
		assertThat(PASSWORD_ENCODER.matches("brand-new-pass", linh.getPasswordHash())).isTrue();
		assertThat(PASSWORD_ENCODER.matches(PASSWORD, linh.getPasswordHash())).isFalse();
	}

	@Test
	void changePassword_wrongCurrentPassword_throwsAndKeepsOldHash() {
		// given
		User linh = user(7L, "linh@corp.com", PASSWORD_HASH, UserType.MANAGER, UserStatus.ACTIVE);
		when(userRepository.findById(7L)).thenReturn(Optional.of(linh));

		// when / then
		assertThatThrownBy(() -> authService.changePassword(7L, new ChangePasswordRequest("wrong", "brand-new-pass")))
				.isInstanceOfSatisfying(InvalidRequestException.class,
						ex -> assertThat(ex.getMessageKey()).isEqualTo(MessageKeys.AUTH_PASSWORD_CURRENT_WRONG));
		assertThat(linh.getPasswordHash()).isEqualTo(PASSWORD_HASH);
	}

	private void assertLoginFails(LoginRequest request) {
		assertThatThrownBy(() -> authService.login(request))
				.isInstanceOfSatisfying(UnauthorizedException.class,
						ex -> assertThat(ex.getMessageKey()).isEqualTo(MessageKeys.AUTH_LOGIN_FAILED));
		verify(tokenService, never()).createToken(any());
	}

	private static Jwt jwt(String value) {
		return Jwt.withTokenValue(value)
				.header("alg", "HS256")
				.subject("7")
				.expiresAt(EXPIRES_AT)
				.build();
	}
}
