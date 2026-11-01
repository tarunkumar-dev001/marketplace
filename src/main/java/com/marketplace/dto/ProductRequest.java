package com.marketplace.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record ProductRequest(
		@NotBlank @Size(max = 150) String name,
		@Size(max = 5000) String description,
		@NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal price,
		@NotNull @Min(0) Integer stockQuantity,
		@NotBlank @Size(max = 100) String category,
		Boolean active,
		@Size(max = 500) @Pattern(regexp = "(https://.*)?") String imageUrl
) {
	public ProductRequest(String name, String description, BigDecimal price, Integer stockQuantity, String category,
			Boolean active) {
		this(name, description, price, stockQuantity, category, active, null);
	}

	public ProductRequest {
		name = name == null ? null : name.trim();
		category = category == null ? null : category.trim();
	}
}
