package com.marketplace.dto;

import com.marketplace.entity.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProductResponseTests {

	@Test
	void fromMapsProductFields() {
		Product product = new Product(
				"Wireless Headphones",
				"Noise-cancelling headphones",
				new BigDecimal("129.99"),
				25,
				"Electronics",
				false
		);

		ProductResponse response = ProductResponse.from(product);

		assertNull(response.id());
		assertEquals("Wireless Headphones", response.name());
		assertEquals("Noise-cancelling headphones", response.description());
		assertEquals(new BigDecimal("129.99"), response.price());
		assertEquals(25, response.stockQuantity());
		assertEquals("Electronics", response.category());
		assertFalse(response.active());
		assertNull(response.createdAt());
		assertNull(response.updatedAt());
	}
}
