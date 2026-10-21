package com.marketplace.security;

import com.marketplace.entity.User;
import com.marketplace.enums.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtServiceTests {

	private static final String ISSUER = "https://marketplace.test";

	private AnnotationConfigApplicationContext context;
	private JwtService jwtService;
	private JwtDecoder jwtDecoder;

	@BeforeEach
	void setUp() {
		byte[] keyBytes = new byte[32];
		new SecureRandom().nextBytes(keyBytes);
		context = new AnnotationConfigApplicationContext();
		context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("testJwt", Map.of(
				"MARKETPLACE_JWT_SECRET_BASE64", Base64.getEncoder().encodeToString(keyBytes),
				"MARKETPLACE_JWT_ISSUER", ISSUER,
				"MARKETPLACE_JWT_ACCESS_TOKEN_TTL_SECONDS", "900"
		)));
		context.register(JwtConfig.class, JwtService.class);
		context.refresh();
		jwtService = context.getBean(JwtService.class);
		jwtDecoder = context.getBean(JwtDecoder.class);
	}

	@AfterEach
	void tearDown() {
		context.close();
	}

	@Test
	void issueAccessTokenCreatesVerifiableTokenWithExpectedClaimsAndExpiry() {
		User user = mock(User.class);
		when(user.getId()).thenReturn(42L);
		when(user.getEmail()).thenReturn("customer@example.com");
		when(user.getRole()).thenReturn(Role.CUSTOMER);

		Instant before = Instant.now();
		JwtService.IssuedToken issuedToken = jwtService.issueAccessToken(user);
		Jwt decoded = jwtDecoder.decode(issuedToken.token());

		assertNotNull(issuedToken.token());
		assertEquals(900, issuedToken.expiresInSeconds());
		assertEquals(ISSUER, decoded.getIssuer().toString());
		assertEquals("42", decoded.getSubject());
		assertEquals("customer@example.com", decoded.getClaimAsString("email"));
		assertEquals("CUSTOMER", decoded.getClaimAsString("role"));
		assertNotNull(decoded.getIssuedAt());
		assertNotNull(decoded.getExpiresAt());
		assertTrue(!decoded.getExpiresAt().isBefore(before.plus(Duration.ofSeconds(899))));
		assertTrue(!decoded.getExpiresAt().isAfter(Instant.now().plus(Duration.ofSeconds(901))));
	}
}
