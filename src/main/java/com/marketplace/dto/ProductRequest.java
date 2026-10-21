package com.marketplace.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
		@NotBlank @Size(max = 150) String name,
		@Size(max = 5000) String description,
		@NotNull @DecimalMin(value = "0.01") BigDecimal price,
		@NotNull @Min(0) Integer stockQuantity,
		@NotBlank @Size(max = 100) String category,
		boolean active
) {
}
