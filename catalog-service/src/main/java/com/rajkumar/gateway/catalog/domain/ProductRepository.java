package com.rajkumar.gateway.catalog.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Uses idx_products_active_category; the entity graph avoids an N+1 on category. */
    @EntityGraph(attributePaths = "category")
    Page<Product> findByActiveTrueAndCategoryId(Long categoryId, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByActiveTrue(Pageable pageable);

    /**
     * No IgnoreCase here on purpose: the column's default MySQL collation (utf8mb4_0900_ai_ci)
     * already compares case-insensitively, and wrapping the column in UPPER() would stop
     * MySQL from using idx_products_active_name for the prefix LIKE.
     */
    @EntityGraph(attributePaths = "category")
    Page<Product> findByActiveTrueAndNameStartingWith(String prefix, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Optional<Product> findWithCategoryById(Long id);

    boolean existsBySku(String sku);
}
