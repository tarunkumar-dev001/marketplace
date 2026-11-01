package com.marketplace.entity;

import com.marketplace.enums.OrderStatus;
import com.marketplace.enums.Role;
import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerOrderTests {

	@Test
	void constructorAssignsPurchaseFieldsAndStartsWithNoItems() {
		User customer = new User("customer@example.com", "password-hash", Role.CUSTOMER, true);

		CustomerOrder order = new CustomerOrder(customer, OrderStatus.PLACED, "INR", new BigDecimal("25.00"));

		assertEquals(customer, order.getCustomer());
		assertEquals(OrderStatus.PLACED, order.getStatus());
		assertEquals("INR", order.getCurrency());
		assertEquals(new BigDecimal("25.00"), order.getTotalAmount());
		assertTrue(order.getItems().isEmpty());
	}

	@Test
	void currencyMappingUsesTheMigrationColumnType() throws NoSuchFieldException {
		Column currency = CustomerOrder.class.getDeclaredField("currency").getAnnotation(Column.class);

		assertEquals("currency", currency.name());
		assertEquals(3, currency.length());
		assertEquals("CHAR(3)", currency.columnDefinition());
	}

	@Test
	void orderStatusDeclaresTheValuesAllowedByTheMigrationConstraint() {
		assertEquals(java.util.List.of("PLACED", "CANCELLED", "FULFILLED"),
				java.util.Arrays.stream(OrderStatus.values()).map(Enum::name).toList());
	}

	@Test
	void cancelChangesOnlyPlacedOrdersToCancelled() {
		User customer = new User("customer@example.com", "password-hash", Role.CUSTOMER, true);
		CustomerOrder placed = new CustomerOrder(customer, OrderStatus.PLACED, "INR", new BigDecimal("25.00"));

		placed.cancel();

		assertEquals(OrderStatus.CANCELLED, placed.getStatus());
		CustomerOrder fulfilled = new CustomerOrder(customer, OrderStatus.FULFILLED, "INR", new BigDecimal("25.00"));
		assertThrows(com.marketplace.exception.OrderCancellationNotAllowedException.class, fulfilled::cancel);
	}
}
