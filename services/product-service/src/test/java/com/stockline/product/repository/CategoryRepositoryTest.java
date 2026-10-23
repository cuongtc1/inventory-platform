package com.stockline.product.repository;

import com.stockline.product.TestcontainersConfiguration;
import com.stockline.product.entity.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the derived queries in {@link CategoryRepository} against MySQL.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class CategoryRepositoryTest {

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private TestEntityManager entityManager;

	private Category electronics;

	@BeforeEach
	void setUp() {
		electronics = entityManager.persist(new Category("Electronics", null));
		entityManager.persist(new Category("Cables", electronics));
		entityManager.persist(new Category("USB Cables", electronics));
		entityManager.persist(new Category("Accessories", null));
		entityManager.flush();
		entityManager.clear();
	}

	@Test
	void findByNameContainingIgnoreCase_partOfName_returnsMatchesWithParent() {
		// when
		Page<Category> page = categoryRepository.findByNameContainingIgnoreCase(
				"CABLE", PageRequest.of(0, 20, Sort.by("name")));

		// then
		assertThat(page.getContent()).extracting(Category::getName).containsExactly("Cables", "USB Cables");
		assertThat(page.getContent().getFirst().getParent().getName()).isEqualTo("Electronics");
	}

	@Test
	void findByNameContainingIgnoreCase_emptyText_returnsAllPaged() {
		// when
		Page<Category> page = categoryRepository.findByNameContainingIgnoreCase("", PageRequest.of(0, 3, Sort.by("name")));

		// then
		assertThat(page.getTotalElements()).isEqualTo(4);
		assertThat(page.getTotalPages()).isEqualTo(2);
		assertThat(page.getContent()).extracting(Category::getName)
				.containsExactly("Accessories", "Cables", "Electronics");
	}

	@Test
	void existsByName_differentCase_returnsTrue() {
		// MySQL's default collation (utf8mb4_0900_ai_ci) ignores case
		assertThat(categoryRepository.existsByName("cables")).isTrue();
		assertThat(categoryRepository.existsByName("Displays")).isFalse();
	}

	@Test
	void existsByNameAndIdNot_sameRow_returnsFalse() {
		assertThat(categoryRepository.existsByNameAndIdNot("Electronics", electronics.getId())).isFalse();
		assertThat(categoryRepository.existsByNameAndIdNot("Electronics", electronics.getId() + 100)).isTrue();
	}

	@Test
	void existsByParentId_parentWithChildren_returnsTrue() {
		assertThat(categoryRepository.existsByParentId(electronics.getId())).isTrue();
	}
}
