package com.stockline.auth.service;

import com.stockline.auth.constant.MessageKeys;
import com.stockline.auth.dto.ChangePasswordRequest;
import com.stockline.auth.dto.LoginRequest;
import com.stockline.auth.dto.LoginResponse;
import com.stockline.auth.dto.UserResponse;
import com.stockline.auth.entity.User;
import com.stockline.auth.mapper.UserMapper;
import com.stockline.auth.repository.UserRepository;
import com.stockline.common.constant.CommonMessageKeys;
import com.stockline.common.exception.InvalidRequestException;
import com.stockline.common.exception.UnauthorizedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

/**
 * What a user does with their own account: log in, see who they are, change their password.
 *
 * <p>Every login failure (unknown email, wrong password, deactivated user) gets the same 401 "Wrong email or
 * password", so the answer doesn't tell an attacker which emails exist.
 */
@Service
@Transactional(readOnly = true)
public class AuthService {

	private static final String TOKEN_TYPE = "Bearer";

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final TokenService tokenService;
	private final UserMapper userMapper;
	/**
	 * A real BCrypt hash of nothing in particular. For an unknown email the password is still checked against
	 * it, so that answer takes as long as for a known email: otherwise the response time would reveal which
	 * emails exist.
	 */
	private final String unknownUserHash;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService,
					   UserMapper userMapper) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
		this.userMapper = userMapper;
		this.unknownUserHash = passwordEncoder.encode("unknown-user");
	}

	public LoginResponse login(LoginRequest request) {
		Optional<User> found = userRepository.findByEmail(normalizeEmail(request.email()));
		String hash = found.map(User::getPasswordHash).orElse(unknownUserHash);
		boolean passwordMatches = passwordEncoder.matches(request.password(), hash);

		User user = found.filter(candidate -> passwordMatches && candidate.isActive())
				.orElseThrow(() -> new UnauthorizedException(MessageKeys.AUTH_LOGIN_FAILED));

		Jwt token = tokenService.createToken(user);
		return new LoginResponse(token.getTokenValue(), TOKEN_TYPE, token.getExpiresAt(),
				new LoginResponse.LoggedInUser(user.getId(), user.getEmail(), user.getFullName(), user.getUserType()));
	}

	/** The signed-in user, read fresh from the database (the token may be hours old). */
	public UserResponse me(Long userId) {
		return userMapper.toResponse(activeUser(userId));
	}

	@Transactional
	public void changePassword(Long userId, ChangePasswordRequest request) {
		User user = activeUser(userId);
		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			throw new InvalidRequestException(MessageKeys.AUTH_PASSWORD_CURRENT_WRONG);
		}
		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
	}

	/** Emails are stored and compared trimmed and in lower case: " Admin@Stockline.local" finds the admin. */
	public static String normalizeEmail(String email) {
		return email.strip().toLowerCase(Locale.ROOT);
	}

	/**
	 * The user behind a valid token. A user deactivated (or removed from the database) after logging in still
	 * has a valid token until it expires; for their own account they get 401 as if they were logged out.
	 */
	private User activeUser(Long userId) {
		return userRepository.findById(userId)
				.filter(User::isActive)
				.orElseThrow(() -> new UnauthorizedException(CommonMessageKeys.AUTH_UNAUTHORIZED));
	}
}
