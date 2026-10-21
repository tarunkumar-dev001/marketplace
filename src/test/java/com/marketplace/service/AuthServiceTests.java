package com.marketplace.service;

import com.marketplace.entity.User;
import com.marketplace.enums.Role;
import com.marketplace.exception.InvalidCredentialsException;
import com.marketplace.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthServiceTests {

	private UserRepository userRepository;
	private PasswordEncoder passwordEncoder;
	private AuthService authService;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		passwordEncoder = mock(PasswordEncoder.class);
		authService = new AuthService(userRepository, passwordEncoder);
	}

	@Test
	void authenticateNormalizesEmailAndReturnsActiveUserWithMatchingPassword() {
		User user = user(true);
		when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("SafePassword123!", "stored-hash")).thenReturn(true);

		User result = authService.authenticate("  CUSTOMER@Example.COM  ", "SafePassword123!");

		assertSame(user, result);
		verify(userRepository).findByEmail("customer@example.com");
		verify(passwordEncoder).matches("SafePassword123!", "stored-hash");
	}

	@Test
	void authenticateRejectsUnknownEmailWithGenericCredentialsFailure() {
		when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.empty());

		assertThrows(InvalidCredentialsException.class,
				() -> authService.authenticate("customer@example.com", "SafePassword123!"));

		verify(userRepository).findByEmail("customer@example.com");
		verifyNoInteractions(passwordEncoder);
	}

	@Test
	void authenticateRejectsIncorrectPasswordWithGenericCredentialsFailure() {
		User user = user(true);
		when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("WrongPassword123!", "stored-hash")).thenReturn(false);

		assertThrows(InvalidCredentialsException.class,
				() -> authService.authenticate("customer@example.com", "WrongPassword123!"));

		verify(passwordEncoder).matches("WrongPassword123!", "stored-hash");
	}

	@Test
	void authenticateRejectsInactiveUserWithGenericCredentialsFailure() {
		User user = user(false);
		when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));

		assertThrows(InvalidCredentialsException.class,
				() -> authService.authenticate("customer@example.com", "SafePassword123!"));

		verify(userRepository).findByEmail("customer@example.com");
		verifyNoInteractions(passwordEncoder);
	}

	@Test
	void authenticateRejectsBlankCredentialsBeforeRepositoryAccess() {
		assertThrows(IllegalArgumentException.class, () -> authService.authenticate(" ", "SafePassword123!"));
		assertThrows(IllegalArgumentException.class, () -> authService.authenticate("customer@example.com", " "));

		verifyNoInteractions(userRepository, passwordEncoder);
	}

	private User user(boolean active) {
		User user = mock(User.class);
		when(user.isActive()).thenReturn(active);
		when(user.getPasswordHash()).thenReturn("stored-hash");
		when(user.getRole()).thenReturn(Role.CUSTOMER);
		return user;
	}
}
