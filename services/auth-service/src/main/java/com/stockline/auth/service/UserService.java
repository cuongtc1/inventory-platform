package com.stockline.auth.service;

import com.stockline.auth.constant.MessageKeys;
import com.stockline.auth.constant.UserStatus;
import com.stockline.auth.constant.UserType;
import com.stockline.auth.dto.CreateUserRequest;
import com.stockline.auth.dto.UpdateUserRequest;
import com.stockline.auth.dto.UserFilter;
import com.stockline.auth.dto.UserResponse;
import com.stockline.auth.entity.User;
import com.stockline.auth.mapper.UserMapper;
import com.stockline.auth.repository.UserRepository;
import com.stockline.auth.repository.UserSpecifications;
import com.stockline.common.dto.PageResponse;
import com.stockline.common.exception.DuplicateResourceException;
import com.stockline.common.exception.InvalidRequestException;
import com.stockline.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User management for ADMINs (the security config only lets ADMIN tokens reach /api/users).
 *
 * <p>Two rules keep an ADMIN from locking themselves (and possibly everyone) out: they can't deactivate their own
 * account, and they can't take the ADMIN role away from themselves. {@code actorId} is the id of the ADMIN making
 * the request, from the token's {@code sub}.
 */
@Service
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final UserMapper userMapper;

	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, UserMapper userMapper) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.userMapper = userMapper;
	}

	public PageResponse<UserResponse> list(UserFilter filter, Pageable pageable) {
		return PageResponse.from(
				userRepository.findAll(UserSpecifications.matching(filter), pageable).map(userMapper::toResponse));
	}

	public UserResponse get(Long id) {
		return userMapper.toResponse(findUser(id));
	}

	@Transactional
	public UserResponse create(CreateUserRequest request) {
		String email = AuthService.normalizeEmail(request.email());
		if (userRepository.existsByEmail(email)) {
			throw duplicateEmail(email);
		}
		User saved = userRepository.save(new User(email, passwordEncoder.encode(request.password()),
				request.fullName().strip(), request.userType(), UserStatus.ACTIVE));
		return userMapper.toResponse(saved);
	}

	@Transactional
	public UserResponse update(Long actorId, Long id, UpdateUserRequest request) {
		User user = findUser(id);
		String email = AuthService.normalizeEmail(request.email());
		if (userRepository.existsByEmailAndIdNot(email, id)) {
			throw duplicateEmail(email);
		}
		if (id.equals(actorId) && request.userType() != UserType.ADMIN) {
			throw new InvalidRequestException(MessageKeys.USER_ROLE_SELF);
		}

		user.setEmail(email);
		user.setFullName(request.fullName().strip());
		user.setUserType(request.userType());
		return flushAndMap(user);
	}

	/**
	 * The user can't log in any more. Tokens they already have stay valid until they expire (at most
	 * JWT_EXPIRATION): a JWT is checked without asking this service. Calling it twice is harmless.
	 */
	@Transactional
	public UserResponse deactivate(Long actorId, Long id) {
		if (id.equals(actorId)) {
			throw new InvalidRequestException(MessageKeys.USER_DEACTIVATE_SELF);
		}
		User user = findUser(id);
		user.setStatus(UserStatus.INACTIVE);
		return flushAndMap(user);
	}

	/** Undoes {@link #deactivate}: the user can log in again with their old password. */
	@Transactional
	public UserResponse activate(Long id) {
		User user = findUser(id);
		user.setStatus(UserStatus.ACTIVE);
		return flushAndMap(user);
	}

	/**
	 * Runs the UPDATE now, so JPA auditing sets updatedAt / updatedBy before the response is built (it fills them
	 * when the change is written, which would otherwise only happen at the end of the transaction).
	 */
	private UserResponse flushAndMap(User user) {
		userRepository.flush();
		return userMapper.toResponse(user);
	}

	private User findUser(Long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(User.class.getSimpleName(), id));
	}

	private static DuplicateResourceException duplicateEmail(String email) {
		return new DuplicateResourceException(MessageKeys.USER_EMAIL_DUPLICATE, email);
	}
}
