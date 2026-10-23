package com.stockline.product.repository;

import com.stockline.product.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data creates the implementation at startup: save, findById, findAll (with paging), delete, ...
 * Methods below are "derived queries": Spring Data builds the SQL from the method name.
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * {@code WHERE LOWER(name) LIKE LOWER('%text%')}; an empty text matches every row.
     * The entity graph loads each parent in the same query (a join) instead of one extra query per row.
     */
    @EntityGraph(attributePaths = "parent")
    Page<Category> findByNameContainingIgnoreCase(String text, Pageable pageable);

    // MySQL's default collation compares case-insensitively, so "cables" matches "Cables" here too
    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsByParentId(Long parentId);
}
