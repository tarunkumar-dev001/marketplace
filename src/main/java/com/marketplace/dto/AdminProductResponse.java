package com.marketplace.dto;

import com.marketplace.entity.Product;
import com.marketplace.entity.User;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminProductResponse(
		Long id,
		String name,
		String description,
		BigDecimal price,
		Integer stockQuantity,
		String category,
		boolean active,
		Long vendorId,
		String vendorEmail,
		Instant createdAt,
		Instant updatedAt
) {

	public static AdminProductResponse from(Product product) {
		User vendor = product.getVendor();
		return new AdminProductResponse(product.getId(), product.getName(), product.getDescription(), product.getPrice(),
				product.getStockQuantity(), product.getCategory(), product.isActive(),
				vendor == null ? null : vendor.getId(), vendor == null ? null : vendor.getEmail(), product.getCreatedAt(),
				product.getUpdatedAt());
	}
}
