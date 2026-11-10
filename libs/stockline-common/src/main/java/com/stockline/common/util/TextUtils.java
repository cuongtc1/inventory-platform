package com.stockline.common.util;

import java.util.Locale;

/**
 * Cleaning up text from requests before it is saved, and turning search text into a LIKE pattern.
 */
public final class TextUtils {

	/**
	 * The escape character of {@link #containsPattern}: pass it to {@code cb.like(expression, pattern, LIKE_ESCAPE)}
	 * in a JPA Specification, so the database knows {@code \%} means a plain "%".
	 */
	public static final char LIKE_ESCAPE = '\\';

	private TextUtils() {
	}

	/**
	 * For optional fields: " Anna " → "Anna"; null, "" and "   " → null, so the table never holds empty strings.
	 */
	public static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}

	/**
	 * For required fields that Bean Validation already checked with {@code @NotBlank}: " Cables " → "Cables".
	 * {@code strip()} (Java 11) also removes Unicode spaces, unlike the older {@code trim()}.
	 */
	public static String strip(String value) {
		return value == null ? null : value.strip();
	}

	/**
	 * A lower-case LIKE pattern for "contains", for a Specification that compares with {@code cb.lower(...)}:
	 * "Mouse" → "%mouse%". {@code %} and {@code _} are LIKE wildcards; they are escaped with {@link #LIKE_ESCAPE}, so
	 * a search for "100%" finds that text instead of everything starting with "100". Spring Data's derived queries
	 * ({@code findByNameContaining}) escape by themselves and don't need this.
	 */
	public static String containsPattern(String text) {
		String escaped = text.toLowerCase(Locale.ROOT)
				.replace("\\", "\\\\")
				.replace("%", "\\%")
				.replace("_", "\\_");
		return "%" + escaped + "%";
	}
}
