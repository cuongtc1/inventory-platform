package com.stockline.common.util;

import java.util.regex.Pattern;

/**
 * Validation rules used by more than one field or resource. Declare a rule here once and point the
 * annotations at it, instead of repeating a regex in every DTO:
 *
 * <pre>
 * &#64;Pattern(regexp = CommonValidationUtils.PHONE_REGEX, message = CommonValidationUtils.PHONE_MESSAGE)
 * </pre>
 *
 * The {@code *_REGEX} and {@code *_MESSAGE} fields are compile-time constants, which annotations require.
 * The {@code is...} methods apply the same rules in plain Java (services, imports, tests).
 */
public final class CommonValidationUtils {

	/** Digits, spaces, "+", "-", "(" and ")": "+84 90 111 2233", "(028) 3800-1122". */
	public static final String PHONE_REGEX = "[0-9+()\\- ]*";
	public static final String PHONE_MESSAGE = "{common.validation.phone.invalid}";

	/**
	 * A code people type and read: letters, digits, ".", "_" and "-", no spaces. Product SKUs ("KB-104"),
	 * later also warehouse codes ("WH-01").
	 */
	public static final String CODE_REGEX = "[A-Za-z0-9._-]*";
	public static final String CODE_MESSAGE = "{common.validation.code.invalid}";

	// Common maximum lengths; they match the VARCHAR sizes in the migrations
	public static final int NAME_MAX_LENGTH = 100;
	public static final int CODE_MAX_LENGTH = 50;
	public static final int EMAIL_MAX_LENGTH = 150;
	public static final int PHONE_MAX_LENGTH = 30;

	private static final Pattern PHONE = Pattern.compile(PHONE_REGEX);
	private static final Pattern CODE = Pattern.compile(CODE_REGEX);

	private CommonValidationUtils() {
	}

	public static boolean isValidPhone(String value) {
		return value != null && PHONE.matcher(value).matches();
	}

	public static boolean isValidCode(String value) {
		return value != null && !value.isEmpty() && CODE.matcher(value).matches();
	}
}
