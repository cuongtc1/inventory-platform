package com.stockline.auth;

import com.stockline.auth.constant.UserStatus;
import com.stockline.auth.entity.User;
import com.stockline.common.constant.UserType;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Ready-made entities for tests. Use with a static import: {@code import static com.stockline.auth.TestData.user;}
 *
 * <p>Methods with an {@code id} are for unit tests, where no database generates ids.
 */
public final class TestData {

	private TestData() {
	}

	public static User user(String email, String passwordHash, UserType userType, UserStatus status) {
		return new User(email, passwordHash, "Test " + userType.name().toLowerCase(), userType, status);
	}

	public static User user(Long id, String email, String passwordHash, UserType userType, UserStatus status) {
		User user = user(email, passwordHash, userType, status);
		// The id is set by the database; unit tests have none, so set the private field directly
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}
}
