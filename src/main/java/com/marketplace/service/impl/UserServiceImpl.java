package com.marketplace.service.impl;

import com.marketplace.entity.User;
import com.marketplace.enums.Role;
import com.marketplace.exception.UserNotFoundException;
import com.marketplace.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

	public Page<User> getAllForAdmin(Pageable pageable, Long actorUserId, Role actorRole) {
		findActiveAdmin(actorUserId, actorRole);
		return userRepository.findAll(pageable);
	}

	@Transactional
	public User updateStatus(Long id, boolean active, Long actorUserId, Role actorRole) {
		findActiveAdmin(actorUserId, actorRole);
		User user = userRepository.findById(id).orElseThrow(UserNotFoundException::new);
		if (!active && user.getId() != null && user.getId().equals(actorUserId)) {
			throw new IllegalArgumentException("An administrator cannot deactivate their own account");
		}
		user.setActive(active);
		return userRepository.save(user);
	}

	private User findActiveAdmin(Long actorUserId, Role actorRole) {
		if (actorUserId == null || actorUserId <= 0 || actorRole != Role.ADMIN) {
			throw new AccessDeniedException("Access denied");
		}
		User user = userRepository.findById(actorUserId).orElseThrow(() -> new AccessDeniedException("Access denied"));
		if (user.getId() == null || !actorUserId.equals(user.getId()) || !user.isActive() || user.getRole() != Role.ADMIN) {
			throw new AccessDeniedException("Access denied");
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
