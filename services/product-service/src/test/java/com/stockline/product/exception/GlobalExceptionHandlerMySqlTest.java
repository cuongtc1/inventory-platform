package com.stockline.product.exception;

import com.stockline.product.TestcontainersConfiguration;
import com.stockline.product.entity.Category;
import com.stockline.product.entity.Product;
import com.stockline.product.entity.ProductStatus;
import com.stockline.product.repository.CategoryRepository;
import com.stockline.product.repository.ProductRepository;
import com.stockline.product.repository.SupplierRepository;
import com.stockline.product.entity.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Feeds real MySQL constraint errors into the handler. A unit test with a hand-made exception can't prove
 * that Hibernate reports the constraint kind, name and error code the way the handler expects.
 *
 * <p>Each test breaks a constraint on purpose, the way two requests at the same moment would.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class GlobalExceptionHandlerMySqlTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler(messageSource());

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private SupplierRepository supplierRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void duplicateSku_returns409DuplicateWithSkuMessage() {
		// given
		Category cables = entityManager.persist(new Category("Cables", null));
		productRepository.saveAndFlush(product("A02", cables));

		// when
		DataIntegrityViolationException ex = catchThrowableOfType(DataIntegrityViolationException.class,
				() -> productRepository.saveAndFlush(product("a02", cables)));
		ResponseEntity<ProblemDetail> response = handle(ex, "/api/products");

		// then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(response.getBody().getProperties()).containsEntry("code", "DUPLICATE");
		assertThat(response.getBody().getDetail()).isEqualTo("A product with this SKU already exists");
	}

	@Test
	void duplicateCategoryName_returns409DuplicateWithCategoryMessage() {
		// given
		categoryRepository.saveAndFlush(new Category("Cables", null));

		// when
		DataIntegrityViolationException ex = catchThrowableOfType(DataIntegrityViolationException.class,
				() -> categoryRepository.saveAndFlush(new Category("CABLES", null)));
		ResponseEntity<ProblemDetail> response = handle(ex, "/api/categories");

		// then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(response.getBody().getDetail()).isEqualTo("A category with this name already exists");
	}

	@Test
	void duplicateSupplierName_returns409DuplicateWithSupplierMessage() {
		// given
		supplierRepository.saveAndFlush(new Supplier("LogiCo", null, null, null));

		// when
		DataIntegrityViolationException ex = catchThrowableOfType(DataIntegrityViolationException.class,
				() -> supplierRepository.saveAndFlush(new Supplier("logico", null, null, null)));
		ResponseEntity<ProblemDetail> response = handle(ex, "/api/suppliers");

		// then
		assertThat(response.getBody().getDetail()).isEqualTo("A supplier with this name already exists");
	}

	@Test
	void deleteCategoryStillUsedByProduct_returns409InUse() {
		// given: the service's "has products" check was passed, then another request added a product
		Category cables = entityManager.persist(new Category("Cables", null));
		productRepository.saveAndFlush(product("A02", cables));
		// Forget the product, as if it was saved by another transaction. Otherwise Hibernate itself
		// refuses the delete (it knows the product points to the category) and MySQL never sees it
		entityManager.clear();

		// when
		DataIntegrityViolationException ex = catchThrowableOfType(DataIntegrityViolationException.class, () -> {
			categoryRepository.deleteById(cables.getId());
			categoryRepository.flush();
		});
		ResponseEntity<ProblemDetail> response = handle(ex, "/api/categories/" + cables.getId());

		// then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(response.getBody().getProperties()).containsEntry("code", "IN_USE");
	}

	@Test
	void productPointingToDeletedCategory_returns404NotFound() {
		// given: the service found the category, then it was deleted before the insert.
		// getReference() gives a stand-in object for id 999999 without checking that the row exists
		Category deleted = entityManager.getEntityManager().getReference(Category.class, 999_999L);

		// when
		DataIntegrityViolationException ex = catchThrowableOfType(DataIntegrityViolationException.class,
				() -> productRepository.saveAndFlush(product("A02", deleted)));
		ResponseEntity<ProblemDetail> response = handle(ex, "/api/products");

		// then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody().getProperties()).containsEntry("code", "NOT_FOUND");
		assertThat(response.getBody().getDetail()).isEqualTo("A referenced record no longer exists");
	}

	private ResponseEntity<ProblemDetail> handle(DataIntegrityViolationException ex, String path) {
		assertThat(ex).as("the database should have rejected the change").isNotNull();
		return handler.handleDataIntegrityViolation(ex, new MockHttpServletRequest("POST", path));
	}

	// @DataJpaTest doesn't create Spring Boot's MessageSource, so read messages.properties directly
	private static MessageSource messageSource() {
		ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
		messageSource.setBasename("messages");
		messageSource.setDefaultEncoding("UTF-8");
		messageSource.setFallbackToSystemLocale(false);
		return messageSource;
	}

	private static Product product(String sku, Category category) {
		return new Product(sku, "Test product", category, null,
				new BigDecimal("1.00"), new BigDecimal("2.00"), 10, ProductStatus.ACTIVE);
	}
}
