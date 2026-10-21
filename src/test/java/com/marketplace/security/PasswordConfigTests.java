package com.marketplace.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordConfigTests {

	@Test
	void passwordEncoderBeanCanBeCreated() {
		PasswordEncoder passwordEncoder = new PasswordConfig().passwordEncoder();

		assertNotNull(passwordEncoder);
	}

	@Test
	void encodedPasswordDiffersFromRawPasswordAndMatchesIt() {
		PasswordEncoder passwordEncoder = new PasswordConfig().passwordEncoder();
		String rawPassword = UUID.randomUUID().toString();

		String encodedPassword = passwordEncoder.encode(rawPassword);

		assertNotEquals(rawPassword, encodedPassword);
		assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
	}

	@Test
	void encodingTheSamePasswordTwiceProducesDifferentMatchingHashes() {
		PasswordEncoder passwordEncoder = new PasswordConfig().passwordEncoder();
		String rawPassword = UUID.randomUUID().toString();

		String firstEncodedPassword = passwordEncoder.encode(rawPassword);
		String secondEncodedPassword = passwordEncoder.encode(rawPassword);

		assertNotEquals(firstEncodedPassword, secondEncodedPassword);
		assertTrue(passwordEncoder.matches(rawPassword, firstEncodedPassword));
		assertTrue(passwordEncoder.matches(rawPassword, secondEncodedPassword));
	}
}
