package com.rajkumar.gateway.catalog.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Body for create (POST) and full update (PUT). {@code sku} is ignored on update. */
public record ProductRequest(
        @NotBlank @Size(max = 40) @Pattern(regexp = "^[A-Z0-9-]+$", message = "upper-case letters, digits and '-' only") String sku,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 5000) String description,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
        @Min(0) int stockQuantity,
        @NotNull Long categoryId,
        Boolean active) {
}
