package com.stockline.product.repository;

import com.stockline.product.TestcontainersConfiguration;
import com.stockline.product.config.JpaAuditingConfig;
import com.stockline.product.entity.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks the derived queries in {@link SupplierRepository} and the unique name against MySQL.
 */
@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class SupplierRepositoryTest {

	@Autowired
	private SupplierRepository supplierRepository;

	@Autowired
	private TestEntityManager entityManager;

	private Supplier logiCo;

	@BeforeEach
	void setUp() {
		logiCo = entityManager.persist(new Supplier("LogiCo", "Anna Pham", "anna@logico.com", "+84 90 111 2233"));
		entityManager.persist(new Supplier("WireCo", "Tom Lee", "tom@wireco.com", null));
		entityManager.persist(new Supplier("ViewTech", null, null, null));
		entityManager.flush();
		entityManager.clear();
	}

	@Test
	void findByNameContainingIgnoreCase_partOfName_returnsMatches() {
		// when
		Page<Supplier> page = supplierRepository.findByNameContainingIgnoreCase(
				"CO", PageRequest.of(0, 20, Sort.by("name")));

		// then
		assertThat(page.getContent()).extracting(Supplier::getName).containsExactly("LogiCo", "WireCo");
	}

	@Test
	void existsByName_differentCase_returnsTrue() {
		assertThat(supplierRepository.existsByName("logico")).isTrue();
		assertThat(supplierRepository.existsByName("DeskPro")).isFalse();
	}

	@Test
	void existsByNameAndIdNot_sameRow_returnsFalse() {
		assertThat(supplierRepository.existsByNameAndIdNot("LogiCo", logiCo.getId())).isFalse();
		assertThat(supplierRepository.existsByNameAndIdNot("LogiCo", logiCo.getId() + 100)).isTrue();
	}

	@Test
	void save_duplicateName_throwsDataIntegrityViolationException() {
		// uk_suppliers_name is the last line of defence if two requests pass the service check at the same time
		assertThatThrownBy(() -> supplierRepository.saveAndFlush(new Supplier("WIRECO", null, null, null)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}
