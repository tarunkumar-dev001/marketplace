package com.marketplace.entity;

import com.marketplace.enums.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartTests {

	@Test
	void constructorAssignsCustomerAndStartsWithNoItems() {
		User customer = new User("customer@example.com", "password-hash", Role.CUSTOMER, true);

		Cart cart = new Cart(customer);

		assertEquals(customer, cart.getCustomer());
		assertTrue(cart.getItems().isEmpty());
	}
}
