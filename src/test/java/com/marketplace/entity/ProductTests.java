package com.marketplace.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductTests {

	@Test
	void constructorAssignsBusinessFields() {
		Product product = new Product(
				"Wireless Headphones",
				"Noise-cancelling headphones",
				new BigDecimal("129.99"),
				25,
				"Electronics",
				false
		);

		assertEquals("Wireless Headphones", product.getName());
		assertEquals("Noise-cancelling headphones", product.getDescription());
		assertEquals(new BigDecimal("129.99"), product.getPrice());
		assertEquals(25, product.getStockQuantity());
		assertEquals("Electronics", product.getCategory());
		assertEquals(false, product.isActive());
	}

	@Test
	void updateChangesBusinessFields() {
		Product product = new Product("Original", "Original description", new BigDecimal("10.00"), 5, "Books", true);

		product.update("Updated", "Updated description", new BigDecimal("15.50"), 8, "Media", false);

		assertEquals("Updated", product.getName());
		assertEquals("Updated description", product.getDescription());
		assertEquals(new BigDecimal("15.50"), product.getPrice());
		assertEquals(8, product.getStockQuantity());
		assertEquals("Media", product.getCategory());
		assertEquals(false, product.isActive());
	}

	@Test
	void noArgumentConstructorPreservesActiveDefault() {
		Product product = new Product();

		assertTrue(product.isActive());
	}

	@Test
	void decrementStockRejectsAmountsThatWouldMakeStockNegative() {
		Product product = new Product("Product", "Description", new BigDecimal("10.00"), 3, "Books", true);

		product.decrementStock(2);

		assertEquals(1, product.getStockQuantity());
		assertThrows(IllegalArgumentException.class, () -> product.decrementStock(2));
		assertEquals(1, product.getStockQuantity());
	}
}
