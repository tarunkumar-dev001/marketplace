package com.marketplace.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InitialSchemaMigrationTests {

	@Test
	void initialSchemaMatchesCurrentUserAndProductMappings() throws IOException {
		String migration = readMigration();
		String users = tableDefinition(migration, "users");
		String products = tableDefinition(migration, "products");

		assertContains(users, "id bigint not null auto_increment");
		assertContains(users, "email varchar(320) not null");
		assertContains(users, "password_hash varchar(255) not null");
		assertContains(users, "role varchar(20) not null");
		assertContains(users, "active bit not null");
		assertContains(users, "created_at datetime(6) not null");
		assertContains(users, "updated_at datetime(6) not null");
		assertContains(users, "primary key (id)");
		assertContains(users, "constraint uk_users_email unique (email)");

		assertContains(products, "id bigint not null auto_increment");
		assertContains(products, "name varchar(150) not null");
		assertContains(products, "description text null");
		assertContains(products, "price decimal(19, 2) not null");
		assertContains(products, "stock_quantity int not null");
		assertContains(products, "category varchar(100) not null");
		assertContains(products, "active bit not null");
		assertContains(products, "created_at datetime(6) not null");
		assertContains(products, "updated_at datetime(6) not null");
		assertContains(products, "primary key (id)");
		assertFalse(products.contains("vendor_id"));
		assertFalse(products.contains("foreign key"));
	}

	private String readMigration() throws IOException {
		var resource = getClass().getClassLoader().getResourceAsStream("db/migration/V1__initial_schema.sql");
		assertNotNull(resource, "Initial schema migration must be on the test classpath");
		try (resource) {
			return new String(resource.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	/**
	 * Extracts one CREATE TABLE block before normalizing case and whitespace, so every assertion is scoped to the
	 * intended table rather than matching text elsewhere in the migration.
	 */
	private String tableDefinition(String migration, String tableName) {
		Pattern pattern = Pattern.compile(
				"CREATE\\s+TABLE\\s+" + Pattern.quote(tableName) + "\\s*\\((.*?)\\)\\s*ENGINE\\s*=\\s*InnoDB\\s*;",
				Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		Matcher matcher = pattern.matcher(migration);
		assertTrue(matcher.find(), "Expected CREATE TABLE definition for " + tableName);
		return normalize(matcher.group(1));
	}

	private void assertContains(String tableDefinition, String expectedDefinition) {
		assertTrue(tableDefinition.contains(normalize(expectedDefinition)),
				() -> "Expected table definition to contain: " + expectedDefinition);
	}

	private String normalize(String value) {
		return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
	}
}
