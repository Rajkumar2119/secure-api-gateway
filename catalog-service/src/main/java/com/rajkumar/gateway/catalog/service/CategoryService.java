package com.rajkumar.gateway.catalog.service;

import com.rajkumar.gateway.catalog.api.CategoryRequest;
import com.rajkumar.gateway.catalog.api.CategoryResponse;
import com.rajkumar.gateway.catalog.config.CacheConfig;
import com.rajkumar.gateway.catalog.domain.Category;
import com.rajkumar.gateway.catalog.domain.CategoryRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categories;

    public CategoryService(CategoryRepository categories) {
        this.categories = categories;
    }

    /** Reference data that rarely changes: the whole list is cached under one key. */
    @Cacheable(cacheNames = CacheConfig.CATEGORIES, key = "'all'")
    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return categories.findAllByOrderByNameAsc().stream().map(CategoryResponse::from).toList();
    }

    @CacheEvict(cacheNames = CacheConfig.CATEGORIES, allEntries = true)
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categories.existsBySlug(request.slug())) {
            throw CatalogException.conflict("Category slug " + request.slug() + " already exists");
        }
        return CategoryResponse.from(categories.save(new Category(request.name(), request.slug())));
    }
}
