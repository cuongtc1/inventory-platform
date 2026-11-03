package com.stockline.auth.repository;

import com.stockline.auth.constant.UserStatus;
import com.stockline.auth.constant.UserType;
import com.stockline.auth.dto.UserFilter;
import com.stockline.auth.entity.User;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds the WHERE clause of the users list from the filters that were sent, like ProductSpecifications.
 */
public final class UserSpecifications {

	private static final char LIKE_ESCAPE = '\\';

	private UserSpecifications() {
	}

	public static Specification<User> matching(UserFilter filter) {
		List<Specification<User>> conditions = new ArrayList<>();
		if (filter.search() != null && !filter.search().isBlank()) {
			conditions.add(emailOrNameContains(filter.search().strip()));
		}
		if (filter.userType() != null) {
			conditions.add(hasUserType(filter.userType()));
		}
		if (filter.status() != null) {
			conditions.add(hasStatus(filter.status()));
		}
		return Specification.allOf(conditions);
	}

	/**
	 * {@code LOWER(email) LIKE '%text%' OR LOWER(full_name) LIKE '%text%'}.
	 */
	static Specification<User> emailOrNameContains(String text) {
		String pattern = "%" + escapeLike(text.toLowerCase(Locale.ROOT)) + "%";
		return (root, query, cb) -> cb.or(
				cb.like(cb.lower(root.get("email")), pattern, LIKE_ESCAPE),
				cb.like(cb.lower(root.get("fullName")), pattern, LIKE_ESCAPE));
	}

	static Specification<User> hasUserType(UserType userType) {
		return (root, query, cb) -> cb.equal(root.get("userType"), userType);
	}

	static Specification<User> hasStatus(UserStatus status) {
		return (root, query, cb) -> cb.equal(root.get("status"), status);
	}

	/** {@code %} and {@code _} are LIKE wildcards; escape them so they are searched as text. */
	private static String escapeLike(String text) {
		return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}
}
