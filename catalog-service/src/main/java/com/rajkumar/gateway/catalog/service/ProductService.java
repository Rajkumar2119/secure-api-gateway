package com.rajkumar.gateway.catalog.service;

import com.rajkumar.gateway.catalog.api.ProductRequest;
import com.rajkumar.gateway.catalog.api.ProductResponse;
import com.rajkumar.gateway.catalog.config.CacheConfig;
import com.rajkumar.gateway.catalog.domain.Category;
import com.rajkumar.gateway.catalog.domain.CategoryRepository;
import com.rajkumar.gateway.catalog.domain.Product;
import com.rajkumar.gateway.catalog.domain.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Single products are cached in Redis by id (read-through). Updates write the new value
 * to the cache, deletes evict it, so readers never see stale data after a change made
 * through the API. Paged listings are not cached: too many key combinations for too little gain.
 */
@Service
public class ProductService {

    private final ProductRepository products;
    private final CategoryRepository categories;

    public ProductService(ProductRepository products, CategoryRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> search(Long categoryId, String namePrefix, Pageable pageable) {
        Page<Product> page;
        if (categoryId != null) {
            page = products.findByActiveTrueAndCategoryId(categoryId, pageable);
        } else if (namePrefix != null && !namePrefix.isBlank()) {
            page = products.findByActiveTrueAndNameStartingWith(namePrefix.trim(), pageable);
        } else {
            page = products.findByActiveTrue(pageable);
        }
        return page.map(ProductResponse::from);
    }

    @Cacheable(cacheNames = CacheConfig.PRODUCTS, key = "#id")
    @Transactional(readOnly = true)
    public ProductResponse get(long id) {
        return products.findWithCategoryById(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> CatalogException.notFound("Product " + id + " not found"));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (products.existsBySku(request.sku())) {
            throw CatalogException.conflict("SKU " + request.sku() + " already exists");
        }
        Product product = new Product(request.sku(), request.name(), request.description(), request.price(),
                request.stockQuantity(), category(request.categoryId()));
        if (Boolean.FALSE.equals(request.active())) {
            product.deactivate();
        }
        return ProductResponse.from(products.saveAndFlush(product));
    }

    @CachePut(cacheNames = CacheConfig.PRODUCTS, key = "#id")
    @Transactional
    public ProductResponse update(long id, ProductRequest request) {
        Product product = products.findWithCategoryById(id)
                .orElseThrow(() -> CatalogException.notFound("Product " + id + " not found"));
        product.update(request.name(), request.description(), request.price(), request.stockQuantity(),
                category(request.categoryId()), request.active() == null || request.active());
        return ProductResponse.from(products.saveAndFlush(product));
    }

    /** Soft delete: the row stays for order history, but the product leaves the catalogue. */
    @CacheEvict(cacheNames = CacheConfig.PRODUCTS, key = "#id")
    @Transactional
    public void delete(long id) {
        Product product = products.findById(id)
                .orElseThrow(() -> CatalogException.notFound("Product " + id + " not found"));
        product.deactivate();
    }

    private Category category(Long categoryId) {
        return categories.findById(categoryId)
                .orElseThrow(() -> CatalogException.badRequest("Category " + categoryId + " does not exist"));
    }
}
