package com.rajkumar.gateway.catalog;

import com.rajkumar.gateway.catalog.api.ProductRequest;
import com.rajkumar.gateway.catalog.api.ProductResponse;
import com.rajkumar.gateway.catalog.service.CategoryService;
import com.rajkumar.gateway.catalog.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real MySQL 8.4 and Redis 7 in Docker (Testcontainers): Flyway migrations and seed
 * data, the indexes the listing query relies on, and the Redis read-through cache.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class CatalogContainersIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void flywaySeedsCategoriesAndProducts() {
        assertThat(categoryService.list()).extracting("slug")
                .containsExactly("books", "electronics", "home-and-kitchen");
        assertThat(productService.search(null, null, PageRequest.of(0, 10, Sort.by("name"))).getTotalElements())
                .isEqualTo(5);
        assertThat(productService.search(null, "wire", PageRequest.of(0, 10)).getContent())
                .extracting(ProductResponse::sku).containsExactly("ELEC-0001");
    }

    @Test
    void listingQueryIsBackedByIndexes() {
        List<Map<String, Object>> indexes = jdbc.queryForList(
                "SELECT DISTINCT index_name FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'products'");
        assertThat(indexes).extracting(row -> row.get("INDEX_NAME") != null ? row.get("INDEX_NAME") : row.get("index_name"))
                .contains("idx_products_active_category_name", "idx_products_active_name", "uk_products_sku");
    }

    @Test
    void productReadsAreCachedInRedisAndUpdatesRefreshTheCache() {
        ProductResponse created = productService.create(new ProductRequest("IT-0001", "Cache Test", null,
                new BigDecimal("10.00"), 1, 1L, true));
        String key = "products::" + created.id();
        assertThat(redisTemplate.hasKey(key)).isFalse();

        ProductResponse first = productService.get(created.id());
        assertThat(redisTemplate.hasKey(key)).isTrue();

        // Change the row behind the cache's back: a cached read still returns the old value...
        jdbc.update("UPDATE products SET name = 'Changed In DB' WHERE id = ?", created.id());
        assertThat(productService.get(created.id()).name()).isEqualTo(first.name());

        // ...while an update through the service writes the new value to the cache.
        productService.update(created.id(), new ProductRequest("IT-0001", "Updated Name", null,
                new BigDecimal("12.00"), 2, 1L, true));
        assertThat(productService.get(created.id()).name()).isEqualTo("Updated Name");

        productService.delete(created.id());
        assertThat(redisTemplate.hasKey(key)).isFalse();
    }
}
