package com.marketplace.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductVendorMigrationTests {

	@Test
	void productVendorMigrationAddsOnlyTheNullableOwnershipRelationship() throws IOException {
		String migration = normalize(readMigration());

		assertTrue(migration.startsWith("alter table products "));
		assertContains(migration, "add column vendor_id bigint null");
		assertContains(migration, "add constraint fk_products_vendor foreign key (vendor_id) references users (id)");
		assertContains(migration, "add index idx_products_vendor_id (vendor_id)");
		assertContains(migration, "on delete restrict on update restrict");
		assertFalse(migration.contains("vendor_id bigint not null"));
		assertFalse(migration.contains("on delete cascade"));
		assertFalse(migration.contains("on update cascade"));
	}

	private String readMigration() throws IOException {
		var resource = getClass().getClassLoader().getResourceAsStream("db/migration/V2__add_product_vendor.sql");
		assertNotNull(resource, "Product vendor migration must be on the test classpath");
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
