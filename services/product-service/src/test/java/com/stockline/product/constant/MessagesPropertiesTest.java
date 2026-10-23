package com.stockline.product.constant;

import com.stockline.common.constant.CommonMessageKeys;
import com.stockline.product.dto.CategoryRequest;
import com.stockline.product.dto.ProductRequest;
import com.stockline.product.dto.SupplierRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keeps messages.properties and the code in step. Each mistake below compiles fine and only shows up when the
 * message is needed, so a test catches it early. Plain JUnit, no Spring.
 */
class MessagesPropertiesTest {

	// DTOs whose Bean Validation annotations may reference message keys, e.g. "{common.validation.code.invalid}"
	private static final List<Class<?>> REQUEST_CLASSES = List.of(
			CategoryRequest.class, SupplierRequest.class, ProductRequest.class);

	// Keys that Hibernate Validator or Spring MVC look up by themselves; no constant in MessageKeys needed
	private static final List<String> FRAMEWORK_KEY_PREFIXES = List.of("jakarta.validation.", "problemDetail.");

	// {0}, {1}, ... = a MessageFormat placeholder
	private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\d+}");
	// An apostrophe that is not part of a doubled '' pair
	private static final Pattern SINGLE_APOSTROPHE = Pattern.compile("(?<!')'(?!')");
	// "CONSTRAINT uk_products_sku UNIQUE" in a Flyway migration
	private static final Pattern UNIQUE_KEY = Pattern.compile("CONSTRAINT (\\w+) UNIQUE");

	// This service's messages.properties, and common-messages.properties from the stockline-common library
	private static Properties messages;
	private static Properties commonMessages;

	@BeforeAll
	static void loadMessages() throws IOException {
		messages = load("/messages.properties");
		commonMessages = load("/common-messages.properties");
	}

	@Test
	void everyMessageKeyConstant_hasAText() {
		assertThat(messageKeyConstants())
				.as("keys in MessageKeys without a line in messages.properties")
				.allSatisfy(key -> assertThat(messages.getProperty(key)).as(key).isNotBlank());
	}

	@Test
	void everyKeyUsedInAValidationAnnotation_hasAText() {
		// Our own keys or the library's (CommonValidationUtils.*_MESSAGE)
		assertThat(annotationKeys())
				.isNotEmpty()
				.allSatisfy(key -> assertThat(messages.getProperty(key, commonMessages.getProperty(key)))
						.as(key).isNotBlank());
	}

	@Test
	void everyKeyInTheFile_isUsed() {
		// given
		Set<String> used = new HashSet<>(messageKeyConstants());
		used.addAll(annotationKeys());

		// then: "constraint.*" keys are looked up by the unique key's name, checked in the next test
		assertThat(messages.stringPropertyNames())
				.filteredOn(key -> FRAMEWORK_KEY_PREFIXES.stream().noneMatch(key::startsWith))
				.filteredOn(key -> !key.startsWith(CommonMessageKeys.CONSTRAINT_PREFIX))
				.as("keys in messages.properties that no code uses")
				.allSatisfy(key -> assertThat(used).contains(key));
	}

	@Test
	void everyConstraintKey_namesAUniqueKeyFromTheMigrations() throws IOException {
		// given
		Set<String> uniqueKeys = new HashSet<>();
		for (Path migration : Files.newDirectoryStream(Path.of("src/main/resources/db/migration"), "*.sql")) {
			UNIQUE_KEY.matcher(Files.readString(migration)).results().forEach(match -> uniqueKeys.add(match.group(1)));
		}

		// then: a typo like "constraint.uk_product_sku" would never be used
		assertThat(uniqueKeys).isNotEmpty();
		assertThat(messages.stringPropertyNames())
				.filteredOn(key -> key.startsWith(CommonMessageKeys.CONSTRAINT_PREFIX))
				.isNotEmpty()
				.allSatisfy(key -> assertThat(uniqueKeys)
						.contains(key.substring(CommonMessageKeys.CONSTRAINT_PREFIX.length())));
	}

	@Test
	void messagesWithPlaceholders_doubleTheirApostrophes() {
		// MessageFormat treats a single ' as "start of literal text": "Category '{0}'" would print "Category {0}"
		assertThat(messages.stringPropertyNames())
				.filteredOn(key -> PLACEHOLDER.matcher(messages.getProperty(key)).find())
				.allSatisfy(key -> assertThat(SINGLE_APOSTROPHE.matcher(messages.getProperty(key)).find())
						.as("%s=%s needs '' instead of '", key, messages.getProperty(key))
						.isFalse());
	}

	@Test
	void messageKeyConstants_followTheSectionNaming() {
		assertThat(messageKeyConstants())
				.allSatisfy(key -> assertThat(key).matches("(category|supplier|product)\\.[a-z.-]+"));
	}

	private static Properties load(String resource) throws IOException {
		Properties properties = new Properties();
		try (InputStream in = MessagesPropertiesTest.class.getResourceAsStream(resource)) {
			assertThat(in).as(resource + " on the classpath").isNotNull();
			properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
		}
		return properties;
	}

	private static List<String> messageKeyConstants() {
		return Stream.of(MessageKeys.class.getDeclaredFields())
				.filter(field -> Modifier.isStatic(field.getModifiers()) && field.getType() == String.class)
				.map(MessagesPropertiesTest::staticValue)
				.toList();
	}

	/**
	 * Reads {@code message} from every constraint annotation on the request records' fields and keeps
	 * our own keys ("{product.sku.invalid}" → "product.sku.invalid").
	 */
	private static Set<String> annotationKeys() {
		Set<String> keys = new HashSet<>();
		for (Class<?> requestClass : REQUEST_CLASSES) {
			for (Field field : requestClass.getDeclaredFields()) {
				for (Annotation annotation : field.getAnnotations()) {
					String message = messageOf(annotation);
					if (message != null && message.startsWith("{") && !message.startsWith("{jakarta.")) {
						keys.add(message.substring(1, message.length() - 1));
					}
				}
			}
		}
		return keys;
	}

	private static String messageOf(Annotation annotation) {
		try {
			Method message = annotation.annotationType().getMethod("message");
			return (String) message.invoke(annotation);
		} catch (NoSuchMethodException e) {
			return null; // not a constraint annotation
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	private static String staticValue(Field field) {
		try {
			return (String) field.get(null);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}
}
