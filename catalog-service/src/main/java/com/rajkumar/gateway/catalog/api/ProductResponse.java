package com.rajkumar.gateway.catalog.api;

import com.rajkumar.gateway.catalog.domain.Product;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(Long id, String sku, String name, String description, BigDecimal price,
                              int stockQuantity, Long categoryId, String categoryName, boolean active,
                              Instant updatedAt, long version) {

    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getSku(), p.getName(), p.getDescription(), p.getPrice(),
                p.getStockQuantity(), p.getCategory().getId(), p.getCategory().getName(), p.isActive(),
                p.getUpdatedAt(), p.getVersion());
    }
}
