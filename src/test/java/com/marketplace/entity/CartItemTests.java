package com.marketplace.entity;

import com.marketplace.enums.Role;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CartItemTests {

	@Test
	void constructorAssignsCartProductAndQuantity() {
		Cart cart = new Cart(new User("customer@example.com", "password-hash", Role.CUSTOMER, true));
		Product product = new Product("Book", "Description", new BigDecimal("10.00"), 3, "Books", true);

		CartItem item = new CartItem(cart, product, 2);

		assertEquals(cart, item.getCart());
		assertEquals(product, item.getProduct());
		assertEquals(2, item.getQuantity());
	}
}
