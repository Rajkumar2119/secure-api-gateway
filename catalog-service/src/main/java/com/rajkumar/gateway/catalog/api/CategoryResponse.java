package com.rajkumar.gateway.catalog.api;

import com.rajkumar.gateway.catalog.domain.Category;

public record CategoryResponse(Long id, String name, String slug) {

    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getSlug());
    }
}
