package com.marketplace.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtConfigTests {

	@Test
	void missingIssuerConfigurationFailsClearly() {
		BeanCreationException exception = assertThrows(BeanCreationException.class,
				() -> createContext(Map.of("MARKETPLACE_JWT_SECRET_BASE64", testSecret())));

		assertTrue(rootCauseMessage(exception).contains("MARKETPLACE_JWT_ISSUER must be configured"));
	}

	@Test
	void blankIssuerConfigurationFailsClearly() {
		BeanCreationException exception = assertThrows(BeanCreationException.class,
				() -> createContext(Map.of(
						"MARKETPLACE_JWT_SECRET_BASE64", testSecret(),
						"MARKETPLACE_JWT_ISSUER", " "
				)));

		assertTrue(rootCauseMessage(exception).contains("MARKETPLACE_JWT_ISSUER must be configured"));
	}

	private AnnotationConfigApplicationContext createContext(Map<String, Object> properties) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("testJwt", properties));
		context.register(JwtConfig.class);
		try {
			context.refresh();
		} finally {
			context.close();
		}
		return context;
	}

	private String testSecret() {
		byte[] keyBytes = new byte[32];
		new SecureRandom().nextBytes(keyBytes);
		return Base64.getEncoder().encodeToString(keyBytes);
	}

	private String rootCauseMessage(Throwable exception) {
		Throwable current = exception;
		while (current.getCause() != null) {
			current = current.getCause();
		}
		return current.getMessage();
	}
}
