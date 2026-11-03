package com.stockline.auth.service;

import com.stockline.auth.constant.MessageKeys;
import com.stockline.auth.dto.LoginRequest;
import com.stockline.auth.dto.LoginResponse;
import com.stockline.auth.entity.User;
import com.stockline.auth.repository.UserRepository;
import com.stockline.common.exception.UnauthorizedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

/**
 * Login: checks the email and password and hands out a token.
 *
 * <p>Every failure (unknown email, wrong password, deactivated user) gets the same 401 "Wrong email or
 * password", so the answer doesn't tell an attacker which emails exist.
 */
@Service
@Transactional(readOnly = true)
public class AuthService {

	private static final String TOKEN_TYPE = "Bearer";

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final TokenService tokenService;
	/**
	 * A real BCrypt hash of nothing in particular. For an unknown email the password is still checked against
	 * it, so that answer takes as long as for a known email: otherwise the response time would reveal which
	 * emails exist.
	 */
	private final String unknownUserHash;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
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

	/** Emails are stored and compared trimmed and in lower case: " Admin@Stockline.local" finds the admin. */
	public static String normalizeEmail(String email) {
		return email.strip().toLowerCase(Locale.ROOT);
	}
}
