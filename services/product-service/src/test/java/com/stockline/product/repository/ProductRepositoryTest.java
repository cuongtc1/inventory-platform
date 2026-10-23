package com.stockline.product.repository;

import com.stockline.product.TestcontainersConfiguration;
import com.stockline.product.config.JpaAuditingConfig;
import com.stockline.product.constant.ProductStatus;
import com.stockline.product.entity.Category;
import com.stockline.product.entity.Product;
import com.stockline.product.entity.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs only the JPA layer (entities, repositories, Flyway) against a Testcontainers MySQL.
 * Each test runs in a transaction that is rolled back at the end, so tests don't see each other's rows.
 */
@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class ProductRepositoryTest {

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void save_validProduct_storesAllColumns() {
		// given
		Category electronics = entityManager.persist(new Category("Electronics", null));
		Category peripherals = entityManager.persist(new Category("Peripherals", electronics));
		Supplier logiCo = entityManager.persist(new Supplier("LogiCo", "Anna Pham", "anna@logico.com", "+84 90 111 2233"));
		Product mouse = new Product("A01", "Wireless Mouse", peripherals, logiCo,
				new BigDecimal("8.00"), new BigDecimal("12.00"), 20, ProductStatus.ACTIVE);

		// when
		Long id = productRepository.saveAndFlush(mouse).getId();
		entityManager.clear(); // forget cached objects so the next read really comes from MySQL
		Product found = productRepository.findById(id).orElseThrow();

		// then
		assertThat(found.getSku()).isEqualTo("A01");
		assertThat(found.getName()).isEqualTo("Wireless Mouse");
		assertThat(found.getCategory().getName()).isEqualTo("Peripherals");
		assertThat(found.getCategory().getParent().getName()).isEqualTo("Electronics");
		assertThat(found.getSupplier().getName()).isEqualTo("LogiCo");
		assertThat(found.getCostPrice()).isEqualByComparingTo("8.00");
		assertThat(found.getSalePrice()).isEqualByComparingTo("12.00");
		assertThat(found.getReorderLevel()).isEqualTo(20);
		assertThat(found.getStatus()).isEqualTo(ProductStatus.ACTIVE);
		assertThat(found.getCreatedAt()).isNotNull();
		assertThat(found.getUpdatedAt()).isEqualTo(found.getCreatedAt());
	}

	@Test
	void save_withoutSupplier_isAllowed() {
		// given
		Category cables = entityManager.persist(new Category("Cables", null));
		Product cable = new Product("A02", "USB-C Cable", cables, null,
				new BigDecimal("1.20"), new BigDecimal("3.50"), 20, ProductStatus.ACTIVE);

		// when
		Long id = productRepository.saveAndFlush(cable).getId();
		entityManager.clear();

		// then
		assertThat(productRepository.findById(id).orElseThrow().getSupplier()).isNull();
	}

	@Test
	void existsByCategoryId_categoryWithAndWithoutProducts() {
		// given
		Category cables = entityManager.persist(new Category("Cables", null));
		Category displays = entityManager.persist(new Category("Displays", null));
		productRepository.saveAndFlush(new Product("A02", "USB-C Cable", cables, null,
				new BigDecimal("1.20"), new BigDecimal("3.50"), 20, ProductStatus.ACTIVE));

		// when / then
		assertThat(productRepository.existsByCategoryId(cables.getId())).isTrue();
		assertThat(productRepository.existsByCategoryId(displays.getId())).isFalse();
	}

	@Test
	void existsBySupplierId_supplierWithAndWithoutProducts() {
		// given
		Category cables = entityManager.persist(new Category("Cables", null));
		Supplier wireCo = entityManager.persist(new Supplier("WireCo", null, null, null));
		Supplier viewTech = entityManager.persist(new Supplier("ViewTech", null, null, null));
		productRepository.saveAndFlush(new Product("A02", "USB-C Cable", cables, wireCo,
				new BigDecimal("1.20"), new BigDecimal("3.50"), 20, ProductStatus.ACTIVE));

		// when / then
		assertThat(productRepository.existsBySupplierId(wireCo.getId())).isTrue();
		assertThat(productRepository.existsBySupplierId(viewTech.getId())).isFalse();
	}

	@Test
	void existsBySku_andIdNot_ignoreCaseAndOwnRow() {
		// given
		Category cables = entityManager.persist(new Category("Cables", null));
		Product cable = productRepository.saveAndFlush(new Product("A02", "USB-C Cable", cables, null,
				new BigDecimal("1.20"), new BigDecimal("3.50"), 20, ProductStatus.ACTIVE));

		// when / then
		assertThat(productRepository.existsBySku("a02")).isTrue();
		assertThat(productRepository.existsBySku("A99")).isFalse();
		assertThat(productRepository.existsBySkuAndIdNot("A02", cable.getId())).isFalse();
		assertThat(productRepository.existsBySkuAndIdNot("A02", cable.getId() + 100)).isTrue();
	}

	@Test
	void save_duplicateSku_throwsDataIntegrityViolationException() {
		// given
		Category cables = entityManager.persist(new Category("Cables", null));
		productRepository.saveAndFlush(new Product("A02", "USB-C Cable", cables, null,
				new BigDecimal("1.20"), new BigDecimal("3.50"), 20, ProductStatus.ACTIVE));
		Product sameSku = new Product("A02", "HDMI Adapter", cables, null,
				new BigDecimal("4.00"), new BigDecimal("7.90"), 25, ProductStatus.ACTIVE);

		// when / then: uk_products_sku rejects the INSERT
		assertThatThrownBy(() -> productRepository.saveAndFlush(sameSku))
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}
