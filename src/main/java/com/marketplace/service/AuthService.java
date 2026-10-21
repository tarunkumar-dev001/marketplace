package com.marketplace.service;

import com.marketplace.entity.User;
import com.marketplace.exception.InvalidCredentialsException;
import com.marketplace.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public User authenticate(String email, String rawPassword) {
		String normalizedEmail = canonicalizeEmail(email);
		requireRawPassword(rawPassword);

		User user = userRepository.findByEmail(normalizedEmail)
				.orElseThrow(InvalidCredentialsException::new);
		if (!user.isActive() || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
			throw new InvalidCredentialsException();
		}
		return user;
	}

	private static String canonicalizeEmail(String email) {
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("Email must not be blank");
		}
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private static void requireRawPassword(String rawPassword) {
		if (rawPassword == null || rawPassword.isBlank()) {
			throw new IllegalArgumentException("Password must not be blank");
		}
	}
}
