package com.marketplace.service.impl;

import com.marketplace.entity.User;
import com.marketplace.enums.Role;
import com.marketplace.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class UserServiceImpl {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public User register(String email, String rawPassword) {
		String normalizedEmail = canonicalizeEmail(email);
		requireRawPassword(rawPassword);

		if (userRepository.existsByEmail(normalizedEmail)) {
			throw new IllegalArgumentException("Email is already registered");
		}

		String passwordHash = passwordEncoder.encode(rawPassword);
		User user = new User(normalizedEmail, passwordHash, Role.CUSTOMER, true);
		return userRepository.save(user);
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
