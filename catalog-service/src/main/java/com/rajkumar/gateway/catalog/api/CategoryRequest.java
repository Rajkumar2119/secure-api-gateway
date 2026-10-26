package com.rajkumar.gateway.catalog.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) @Pattern(regexp = "^[a-z0-9-]+$") String slug) {
}
