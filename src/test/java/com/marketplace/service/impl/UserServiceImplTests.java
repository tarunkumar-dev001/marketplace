package com.marketplace.service.impl;

import com.marketplace.entity.User;
import com.marketplace.enums.Role;
import com.marketplace.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
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

	@Test
	void activeAdminCanListUsersAndUpdateUserStatus() {
		User admin = activeUser(1L, Role.ADMIN);
		User customer = new User("customer@example.com", "hash", Role.CUSTOMER, true);
		Page<User> users = new PageImpl<>(List.of(customer), PageRequest.of(0, 20), 1);
		when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
		when(userRepository.findAll(PageRequest.of(0, 20))).thenReturn(users);
		when(userRepository.findById(2L)).thenReturn(Optional.of(customer));
		when(userRepository.save(customer)).thenReturn(customer);

		assertSame(users, userService.getAllForAdmin(PageRequest.of(0, 20), 1L, Role.ADMIN));
		assertSame(customer, userService.updateStatus(2L, false, 1L, Role.ADMIN));
		assertEquals(false, customer.isActive());
	}

	@Test
	void nonAdminsCannotUseAdminUserOperations() {
		assertThrows(AccessDeniedException.class, () -> userService.getAllForAdmin(PageRequest.of(0, 20), 1L, Role.VENDOR));
		assertThrows(AccessDeniedException.class, () -> userService.updateStatus(2L, true, 1L, Role.CUSTOMER));
		verifyNoInteractions(userRepository, passwordEncoder);
	}

	private User activeUser(long id, Role role) {
		User user = mock(User.class);
		when(user.getId()).thenReturn(id);
		when(user.getRole()).thenReturn(role);
		when(user.isActive()).thenReturn(true);
		return user;
	}
}
