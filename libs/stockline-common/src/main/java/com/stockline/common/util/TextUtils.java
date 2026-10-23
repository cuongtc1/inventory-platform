package com.stockline.common.util;

/**
 * Cleaning up text from requests before it is saved.
 */
public final class TextUtils {

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
}
