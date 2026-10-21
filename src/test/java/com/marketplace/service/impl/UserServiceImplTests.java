package com.marketplace.service.impl;

import com.marketplace.entity.User;
import com.marketplace.enums.Role;
import com.marketplace.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserServiceImplTests {

	private UserRepository userRepository;
	private PasswordEncoder passwordEncoder;
	private UserServiceImpl userService;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		passwordEncoder = mock(PasswordEncoder.class);
		userService = new UserServiceImpl(userRepository, passwordEncoder);
	}

	@Test
	void registerNormalizesEmailHashesPasswordAndSavesActiveCustomerUser() {
		String rawPassword = UUID.randomUUID().toString();
		String passwordHash = UUID.randomUUID().toString();
		when(userRepository.existsByEmail("vendor@example.com")).thenReturn(false);
		when(passwordEncoder.encode(rawPassword)).thenReturn(passwordHash);
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		User result = userService.register("  VENDOR@Example.COM  ", rawPassword);

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());
		User savedUser = userCaptor.getValue();
		assertSame(savedUser, result);
		assertEquals("vendor@example.com", savedUser.getEmail());
		assertEquals(passwordHash, savedUser.getPasswordHash());
		assertEquals(Role.CUSTOMER, savedUser.getRole());
		assertTrue(savedUser.isActive());
		verify(userRepository).existsByEmail("vendor@example.com");
		verify(passwordEncoder).encode(rawPassword);
	}

	@Test
	void registerRejectsDuplicateEmailWithoutEncodingOrSaving() {
		String rawPassword = UUID.randomUUID().toString();
		when(userRepository.existsByEmail("customer@example.com")).thenReturn(true);

		assertThrows(IllegalArgumentException.class,
				() -> userService.register(" CUSTOMER@example.com ", rawPassword));

		verify(userRepository).existsByEmail("customer@example.com");
		verify(userRepository, never()).save(any(User.class));
		verifyNoInteractions(passwordEncoder);
	}

	@Test
	void registerRejectsInvalidRequiredInputsWithoutRepositorySave() {
		String rawPassword = UUID.randomUUID().toString();

		assertThrows(IllegalArgumentException.class, () -> userService.register(null, rawPassword));
		assertThrows(IllegalArgumentException.class, () -> userService.register(" ", rawPassword));
		assertThrows(IllegalArgumentException.class, () -> userService.register("user@example.com", null));
		assertThrows(IllegalArgumentException.class, () -> userService.register("user@example.com", " "));

		verify(userRepository, never()).save(any(User.class));
		verifyNoInteractions(userRepository, passwordEncoder);
	}
}
