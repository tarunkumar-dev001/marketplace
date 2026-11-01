package com.marketplace.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartAndOrderMigrationTests {

	@Test
	void cartAndOrderMigrationDefinesThePhaseOneTablesAndConstraints() throws IOException {
		String migration = normalize(readMigration());

		assertContains(migration, "create table carts");
		assertContains(migration, "constraint uk_carts_customer unique (customer_id)");
		assertContains(migration, "create table cart_items");
		assertContains(migration, "constraint uk_cart_items_cart_product unique (cart_id, product_id)");
		assertContains(migration, "constraint chk_cart_items_quantity_positive check (quantity > 0)");
		assertContains(migration, "create table customer_orders");
		assertContains(migration,
				"constraint chk_customer_orders_status check (status in ('placed', 'cancelled', 'fulfilled'))");
		assertContains(migration, "currency char(3) not null");
		assertContains(migration, "total_amount decimal(19, 2) not null");
		assertContains(migration, "version bigint not null");
		assertContains(migration, "create table order_items");
		assertContains(migration, "vendor_id bigint null");
		assertContains(migration, "product_name varchar(150) not null");
		assertContains(migration, "unit_price decimal(19, 2) not null");
		assertContains(migration, "line_total decimal(19, 2) not null");
		assertContains(migration, "constraint chk_order_items_quantity_positive check (quantity > 0)");
	}

	private String readMigration() throws IOException {
		var resource = getClass().getClassLoader().getResourceAsStream("db/migration/V3__add_cart_and_order_tables.sql");
		assertNotNull(resource, "Cart and order migration must be on the test classpath");
		try (resource) {
			return new String(resource.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private void assertContains(String migration, String expectedDefinition) {
		assertTrue(migration.contains(normalize(expectedDefinition)),
				() -> "Expected migration to contain: " + expectedDefinition);
	}

	private String normalize(String value) {
		return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
	}
}
