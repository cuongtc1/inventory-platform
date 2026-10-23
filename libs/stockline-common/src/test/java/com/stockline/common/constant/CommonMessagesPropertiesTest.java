package com.stockline.common.constant;

import com.stockline.common.util.CommonValidationUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keeps common-messages.properties and the library's code in step. Each mistake below compiles fine and only
 * shows up when the message is needed, so a test catches it early.
 */
class CommonMessagesPropertiesTest {

	// Keys that Hibernate Validator or Spring MVC look up by themselves
	private static final List<String> FRAMEWORK_KEY_PREFIXES = List.of("jakarta.validation.", "problemDetail.");

	private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\d+}");
	private static final Pattern SINGLE_APOSTROPHE = Pattern.compile("(?<!')'(?!')");

	private static Properties messages;

	@BeforeAll
	static void loadMessages() throws IOException {
		messages = new Properties();
		try (InputStream in = CommonMessagesPropertiesTest.class.getResourceAsStream("/common-messages.properties")) {
			assertThat(in).as("common-messages.properties on the classpath").isNotNull();
			messages.load(new InputStreamReader(in, StandardCharsets.UTF_8));
		}
	}

	@Test
	void everyCommonMessageKeyConstant_hasAText() {
		assertThat(stringConstants(CommonMessageKeys.class))
				.filteredOn(key -> !key.equals(CommonMessageKeys.CONSTRAINT_PREFIX))
				.allSatisfy(key -> assertThat(messages.getProperty(key)).as(key).isNotBlank());
	}

	@Test
	void everyValidationMessageConstant_hasAText() {
		assertThat(validationMessageKeys())
				.isNotEmpty()
				.allSatisfy(key -> assertThat(messages.getProperty(key)).as(key).isNotBlank());
	}

	@Test
	void everyKeyInTheFile_isUsed() {
		Set<String> used = new HashSet<>(stringConstants(CommonMessageKeys.class));
		used.addAll(validationMessageKeys());

		assertThat(messages.stringPropertyNames())
				.filteredOn(key -> FRAMEWORK_KEY_PREFIXES.stream().noneMatch(key::startsWith))
				.allSatisfy(key -> assertThat(used).as("unused key").contains(key));
	}

	@Test
	void messagesWithPlaceholders_doubleTheirApostrophes() {
		assertThat(messages.stringPropertyNames())
				.filteredOn(key -> PLACEHOLDER.matcher(messages.getProperty(key)).find())
				.allSatisfy(key -> assertThat(SINGLE_APOSTROPHE.matcher(messages.getProperty(key)).find())
						.as("%s=%s needs '' instead of '", key, messages.getProperty(key))
						.isFalse());
	}

	/**
	 * "{common.validation.phone.invalid}" → "common.validation.phone.invalid", from CommonValidationUtils.*_MESSAGE.
	 */
	private static Set<String> validationMessageKeys() {
		Set<String> keys = new HashSet<>();
		for (String value : stringConstants(CommonValidationUtils.class)) {
			if (value.startsWith("{") && value.endsWith("}")) {
				keys.add(value.substring(1, value.length() - 1));
			}
		}
		return keys;
	}

	private static List<String> stringConstants(Class<?> type) {
		return Stream.of(type.getDeclaredFields())
				.filter(field -> Modifier.isStatic(field.getModifiers()) && Modifier.isPublic(field.getModifiers()))
				.filter(field -> field.getType() == String.class)
				.map(CommonMessagesPropertiesTest::staticValue)
				.toList();
	}

	private static String staticValue(Field field) {
		try {
			return (String) field.get(null);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}
}
