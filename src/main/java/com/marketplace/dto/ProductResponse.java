package com.marketplace.dto;

import com.marketplace.entity.Product;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
		Long id,
		String name,
		String description,
		BigDecimal price,
		Integer stockQuantity,
		String category,
		boolean active,
		Instant createdAt,
		Instant updatedAt
) {

	public static ProductResponse from(Product product) {
		return new ProductResponse(
				product.getId(),
				product.getName(),
				product.getDescription(),
				product.getPrice(),
				product.getStockQuantity(),
				product.getCategory(),
				product.isActive(),
				product.getCreatedAt(),
				product.getUpdatedAt()
		);
	}
}
