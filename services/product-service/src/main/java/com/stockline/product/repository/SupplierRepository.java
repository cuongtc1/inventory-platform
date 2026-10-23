package com.stockline.product.repository;

import com.stockline.product.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Derived queries, same as {@link CategoryRepository}. No entity graph needed: a supplier has no relations to load.
 */
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Page<Supplier> findByNameContainingIgnoreCase(String text, Pageable pageable);

    // Case-insensitive through MySQL's default collation
    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
