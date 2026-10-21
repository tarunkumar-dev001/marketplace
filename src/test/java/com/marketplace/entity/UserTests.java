package com.marketplace.entity;

import com.marketplace.enums.Role;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTests {

	@Test
	void constructorAssignsBusinessFieldsAndCanonicalizesEmail() {
		User user = new User("  CUSTOMER@Example.COM  ", "encoded-password-hash", Role.CUSTOMER, false);

		assertNull(user.getId());
		assertEquals("customer@example.com", user.getEmail());
		assertEquals("encoded-password-hash", user.getPasswordHash());
		assertEquals(Role.CUSTOMER, user.getRole());
		assertFalse(user.isActive());
		assertNull(user.getCreatedAt());
		assertNull(user.getUpdatedAt());
	}

	@Test
	void updateRoleChangesOnlyRole() {
		User user = user();

		user.updateRole(Role.ADMIN);

		assertEquals(Role.ADMIN, user.getRole());
		assertEquals("vendor@example.com", user.getEmail());
		assertEquals("initial-password-hash", user.getPasswordHash());
		assertTrue(user.isActive());
	}

	@Test
	void setActiveChangesOnlyActiveStatus() {
		User user = user();

		user.setActive(false);

		assertFalse(user.isActive());
		assertEquals("vendor@example.com", user.getEmail());
		assertEquals("initial-password-hash", user.getPasswordHash());
		assertEquals(Role.VENDOR, user.getRole());
	}

	@Test
	void changePasswordHashChangesOnlyPasswordHash() {
		User user = user();

		user.changePasswordHash("replacement-password-hash");

		assertEquals("replacement-password-hash", user.getPasswordHash());
		assertEquals("vendor@example.com", user.getEmail());
		assertEquals(Role.VENDOR, user.getRole());
		assertTrue(user.isActive());
	}

	@Test
	void noArgumentConstructorPreservesActiveDefault() {
		User user = new User();

		assertTrue(user.isActive());
	}

	@Test
	void timestampCallbacksInitializeAndRefreshTimestamps() {
		User user = user();

		user.onCreate();
		Instant createdAt = user.getCreatedAt();
		Instant initialUpdatedAt = user.getUpdatedAt();

		assertNotNull(createdAt);
		assertEquals(createdAt, initialUpdatedAt);

		user.onUpdate();

		assertNotNull(user.getUpdatedAt());
		assertFalse(user.getUpdatedAt().isBefore(initialUpdatedAt));
	}

	@Test
	void constructorRejectsInvalidRequiredBusinessFields() {
		assertThrows(IllegalArgumentException.class, () -> new User(null, "hash", Role.CUSTOMER, true));
		assertThrows(IllegalArgumentException.class, () -> new User(" ", "hash", Role.CUSTOMER, true));
		assertThrows(IllegalArgumentException.class, () -> new User("user@example.com", null, Role.CUSTOMER, true));
		assertThrows(IllegalArgumentException.class, () -> new User("user@example.com", " ", Role.CUSTOMER, true));
		assertThrows(IllegalArgumentException.class, () -> new User("user@example.com", "hash", null, true));
	}

	private User user() {
		return new User("vendor@example.com", "initial-password-hash", Role.VENDOR, true);
	}
}
