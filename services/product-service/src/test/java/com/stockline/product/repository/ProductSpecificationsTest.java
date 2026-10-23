package com.stockline.product.repository;

import com.stockline.product.TestcontainersConfiguration;
import com.stockline.product.entity.Category;
import com.stockline.product.entity.Product;
import com.stockline.product.entity.ProductStatus;
import com.stockline.product.entity.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the product list filters against MySQL with the mockup's sample products.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class ProductSpecificationsTest {

	private static final PageRequest BY_SKU = PageRequest.of(0, 20, Sort.by("sku"));

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private TestEntityManager entityManager;

	private Category peripherals;
	private Category cables;
	private Supplier logiCo;
	private Supplier wireCo;

	@BeforeEach
	void setUp() {
		peripherals = entityManager.persist(new Category("Peripherals", null));
		cables = entityManager.persist(new Category("Cables", null));
		Category displays = entityManager.persist(new Category("Displays", null));
		logiCo = entityManager.persist(new Supplier("LogiCo", null, null, null));
		wireCo = entityManager.persist(new Supplier("WireCo", null, null, null));

		persist("A01", "Wireless Mouse", peripherals, logiCo, ProductStatus.ACTIVE);
		persist("A02", "USB-C Cable", cables, wireCo, ProductStatus.ACTIVE);
		persist("A03", "Mechanical Keyboard", peripherals, logiCo, ProductStatus.INACTIVE);
		persist("A04", "27\" Monitor", displays, null, ProductStatus.ACTIVE);
		persist("A05", "HDMI Adapter 100%", cables, wireCo, ProductStatus.ACTIVE);
		persist("CAB-9", "Cable Tie", cables, null, ProductStatus.ACTIVE);
		entityManager.flush();
		entityManager.clear();
	}

	@Test
	void matching_noFilters_returnsAllProducts() {
		assertThat(skus(ProductSpecifications.matching(null, null, null, null)))
				.containsExactly("A01", "A02", "A03", "A04", "A05", "CAB-9");
	}

	@Test
	void matching_blankSearch_isIgnored() {
		assertThat(skus(ProductSpecifications.matching("   ", null, null, null))).hasSize(6);
	}

	@Test
	void matching_search_matchesNameOrSkuIgnoringCase() {
		// "cab" is in the names "USB-C Cable" and "Cable Tie", and in the SKU "CAB-9"
		assertThat(skus(ProductSpecifications.matching("cab", null, null, null)))
				.containsExactly("A02", "CAB-9");
		assertThat(skus(ProductSpecifications.matching("a03", null, null, null)))
				.containsExactly("A03");
	}

	@Test
	void matching_searchWithPercent_treatsItAsText() {
		// Without escaping, "0%" would mean "0 followed by anything" and also match "A01", "A02", ...
		assertThat(skus(ProductSpecifications.matching("0%", null, null, null)))
				.containsExactly("A05");
	}

	@Test
	void matching_category_returnsOnlyThatCategory() {
		assertThat(skus(ProductSpecifications.matching(null, cables.getId(), null, null)))
				.containsExactly("A02", "A05", "CAB-9");
	}

	@Test
	void matching_supplier_returnsOnlyThatSupplier() {
		assertThat(skus(ProductSpecifications.matching(null, null, logiCo.getId(), null)))
				.containsExactly("A01", "A03");
	}

	@Test
	void matching_status_returnsOnlyThatStatus() {
		assertThat(skus(ProductSpecifications.matching(null, null, null, ProductStatus.INACTIVE)))
				.containsExactly("A03");
	}

	@Test
	void matching_allFilters_combinesThemWithAnd() {
		assertThat(skus(ProductSpecifications.matching("a", cables.getId(), wireCo.getId(), ProductStatus.ACTIVE)))
				.containsExactly("A02", "A05");
		assertThat(skus(ProductSpecifications.matching("mouse", peripherals.getId(), null, ProductStatus.INACTIVE)))
				.isEmpty();
	}

	@Test
	void findAll_sortByCategoryName_sortsByRelatedField() {
		// when
		Page<Product> page = productRepository.findAll(ProductSpecifications.matching(null, null, null, null),
				PageRequest.of(0, 3, Sort.by("category.name").and(Sort.by("sku"))));

		// then: Cables (A02, A05, CAB-9) comes before Displays and Peripherals
		assertThat(page.getContent()).extracting(Product::getSku).containsExactly("A02", "A05", "CAB-9");
		assertThat(page.getTotalElements()).isEqualTo(6);
		assertThat(page.getTotalPages()).isEqualTo(2);
	}

	private List<String> skus(Specification<Product> spec) {
		return productRepository.findAll(spec, BY_SKU).map(Product::getSku).getContent();
	}

	private void persist(String sku, String name, Category category, Supplier supplier, ProductStatus status) {
		entityManager.persist(new Product(sku, name, category, supplier,
				new BigDecimal("1.00"), new BigDecimal("2.00"), 10, status));
	}
}
