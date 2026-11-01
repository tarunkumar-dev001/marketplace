package com.marketplace.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityConfigTests {

	private static final String ISSUER = "https://marketplace-security.test";

	private AnnotationConfigWebApplicationContext context;
	private MockMvc mockMvc;
	private JwtEncoder jwtEncoder;

	@BeforeEach
	void setUp() {
		byte[] keyBytes = new byte[32];
		new SecureRandom().nextBytes(keyBytes);
		context = new AnnotationConfigWebApplicationContext();
		context.setServletContext(new MockServletContext());
		context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("testJwt", Map.of(
				"MARKETPLACE_JWT_SECRET_BASE64", Base64.getEncoder().encodeToString(keyBytes),
				"MARKETPLACE_JWT_ISSUER", ISSUER
		)));
		context.register(JwtConfig.class, SecurityConfig.class, TestWebConfig.class);
		context.refresh();
		jwtEncoder = context.getBean(JwtEncoder.class);
		mockMvc = MockMvcBuilders.webAppContextSetup(context)
				.apply(springSecurity())
				.build();
	}

	@AfterEach
	void tearDown() {
		context.close();
	}

	@Test
	void registrationLoginAndProductBrowseRoutesArePublic() throws Exception {
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/products"))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/products/{id}", 1L))
				.andExpect(status().isOk());
	}

	@Test
	void viteDevelopmentOriginIsAllowedForApiRequestsAndPreflight() throws Exception {
		mockMvc.perform(get("/api/products").header("Origin", "http://localhost:5173"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

		mockMvc.perform(options("/api/cart/items")
					.header("Origin", "http://localhost:5173")
					.header("Access-Control-Request-Method", "POST")
					.header("Access-Control-Request-Headers", "Authorization, Content-Type"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
				.andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
	}

	@Test
	void unapprovedCorsOriginIsRejected() throws Exception {
		mockMvc.perform(options("/api/cart/items")
					.header("Origin", "http://localhost:4173")
					.header("Access-Control-Request-Method", "POST"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
	}

	@Test
	void protectedRoutesRequireAuthenticationWithSafeUnauthorizedResponse() throws Exception {
		mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Authentication required"));
		mockMvc.perform(put("/api/products/{id}", 1L).contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Authentication required"));
		mockMvc.perform(delete("/api/products/{id}", 1L))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Authentication required"));
		mockMvc.perform(get("/api/cart"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Authentication required"));
		mockMvc.perform(post("/api/cart/items"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Authentication required"));
		mockMvc.perform(put("/api/cart/items/{itemId}", 1L))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Authentication required"));
		mockMvc.perform(delete("/api/cart/items/{itemId}", 1L))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Authentication required"));
		mockMvc.perform(post("/api/cart/checkout"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Authentication required"));
		mockMvc.perform(get("/api/orders"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Authentication required"));
		mockMvc.perform(get("/api/orders/{orderId}", 1L))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Authentication required"));
		mockMvc.perform(post("/api/orders/{orderId}/cancel", 1L))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Authentication required"));
	}

	@Test
	void validBearerTokensForAllSupportedRolesMapAuthorities() throws Exception {
		for (String role : new String[]{"CUSTOMER", "VENDOR", "ADMIN"}) {
			mockMvc.perform(get("/api/role-info")
						.header("Authorization", "Bearer " + token("7", role, ISSUER, Instant.now().plusSeconds(900))))
					.andExpect(status().isOk())
					.andExpect(content().string(containsString("ROLE_" + role)));
		}
	}

	@Test
	void customerTokensAreForbiddenFromAllProductWrites() throws Exception {
		for (String role : new String[]{"CUSTOMER"}) {
			String bearerToken = "Bearer " + token("7", role, ISSUER, Instant.now().plusSeconds(900));
			mockMvc.perform(post("/api/products").header("Authorization", bearerToken))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.message").value("Access denied"));
			mockMvc.perform(put("/api/products/{id}", 1L).header("Authorization", bearerToken))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.message").value("Access denied"));
			mockMvc.perform(delete("/api/products/{id}", 1L).header("Authorization", bearerToken))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.message").value("Access denied"));
		}
	}

	@Test
	void vendorAndAdminTokensPassProductWriteAuthorizationRules() throws Exception {
		for (String role : new String[]{"VENDOR", "ADMIN"}) {
			String bearerToken = "Bearer " + token("7", role, ISSUER, Instant.now().plusSeconds(900));
			mockMvc.perform(post("/api/products").header("Authorization", bearerToken))
					.andExpect(status().isOk())
					.andExpect(content().string(containsString("ROLE_" + role)));
			mockMvc.perform(put("/api/products/{id}", 1L).header("Authorization", bearerToken))
					.andExpect(status().isOk());
			mockMvc.perform(delete("/api/products/{id}", 1L).header("Authorization", bearerToken))
					.andExpect(status().isOk());
		}
	}

	@Test
	void vendorAndAdminTokensAreForbiddenFromAllCartRoutes() throws Exception {
		for (String role : new String[]{"VENDOR", "ADMIN"}) {
			String bearerToken = "Bearer " + token("7", role, ISSUER, Instant.now().plusSeconds(900));
			mockMvc.perform(get("/api/cart").header("Authorization", bearerToken))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.message").value("Access denied"));
			mockMvc.perform(post("/api/cart/items").header("Authorization", bearerToken))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.message").value("Access denied"));
			mockMvc.perform(put("/api/cart/items/{itemId}", 1L).header("Authorization", bearerToken))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.message").value("Access denied"));
			mockMvc.perform(delete("/api/cart/items/{itemId}", 1L).header("Authorization", bearerToken))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.message").value("Access denied"));
			mockMvc.perform(post("/api/cart/checkout").header("Authorization", bearerToken))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.message").value("Access denied"));
			mockMvc.perform(get("/api/orders").header("Authorization", bearerToken))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.message").value("Access denied"));
			mockMvc.perform(get("/api/orders/{orderId}", 1L).header("Authorization", bearerToken))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.message").value("Access denied"));
			mockMvc.perform(post("/api/orders/{orderId}/cancel", 1L).header("Authorization", bearerToken))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.message").value("Access denied"));
		}
	}

	@Test
	void malformedTamperedAndExpiredTokensAreRejectedSafely() throws Exception {
		mockMvc.perform(post("/api/products").header("Authorization", "Bearer not-a-jwt"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Authentication required"));

		String validToken = token("7", "CUSTOMER", ISSUER, Instant.now().plusSeconds(900));
		int signatureStart = validToken.lastIndexOf('.') + 1;
		char originalCharacter = validToken.charAt(signatureStart);
		char replacementCharacter = originalCharacter == 'a' ? 'b' : 'a';
		String tamperedToken = validToken.substring(0, signatureStart)
				+ replacementCharacter + validToken.substring(signatureStart + 1);
		mockMvc.perform(post("/api/products").header("Authorization", "Bearer " + tamperedToken))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Authentication required"));

		mockMvc.perform(post("/api/products")
					.header("Authorization", "Bearer " + token("7", "CUSTOMER", ISSUER, Instant.now().minusSeconds(60))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Authentication required"));
	}

	@Test
	void missingBlankMalformedAndNonPositiveSubjectClaimsAreRejected() throws Exception {
		for (String subject : new String[]{null, "", "abc", "0", "-1"}) {
			assertUnauthorized(token(subject, "CUSTOMER", ISSUER, Instant.now().plusSeconds(900)));
		}
	}

	@Test
	void missingBlankAndUnsupportedRoleClaimsAreRejected() throws Exception {
		assertUnauthorized(token("7", null, ISSUER, Instant.now().plusSeconds(900)));
		assertUnauthorized(token("7", "", ISSUER, Instant.now().plusSeconds(900)));
		assertUnauthorized(token("7", "MANAGER", ISSUER, Instant.now().plusSeconds(900)));
	}

	@Test
	void correctlySignedTokenWithWrongIssuerIsRejected() throws Exception {
		assertUnauthorized(token("7", "CUSTOMER", "https://other-issuer.test", Instant.now().plusSeconds(900)));
	}

	private void assertUnauthorized(String token) throws Exception {
		mockMvc.perform(post("/api/products").header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Authentication required"));
	}

	private String token(String subject, String role, String issuer, Instant expiresAt) {
		Instant issuedAt = expiresAt.isBefore(Instant.now())
				? expiresAt.minusSeconds(60)
				: Instant.now().minusSeconds(1);
		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
				.issuer(issuer)
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.claim("email", "vendor@example.com");
		if (subject != null) {
			claims.subject(subject);
		}
		if (role != null) {
			claims.claim("role", role);
		}
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
	}

	@Configuration
	@EnableWebMvc
	static class TestWebConfig {

		@Bean
		TestController testController() {
			return new TestController();
		}
	}

	@RestController
	static class TestController {

		@PostMapping("/api/auth/register")
		String register() {
			return "registered";
		}

		@PostMapping("/api/auth/login")
		String login() {
			return "logged in";
		}

		@GetMapping("/api/products")
		String getProducts() {
			return "products";
		}

		@GetMapping("/api/products/{id}")
		String getProduct(@PathVariable Long id) {
			return "product";
		}

		@GetMapping("/api/role-info")
		String roleInfo(Authentication authentication) {
			return authorities(authentication);
		}

		@PostMapping("/api/products")
		String createProduct(Authentication authentication) {
			return authorities(authentication);
		}

		@DeleteMapping("/api/products/{id}")
		String deleteProduct(@PathVariable Long id) {
			return "deleted";
		}

		private String authorities(Authentication authentication) {
			return authentication.getAuthorities().stream()
					.map(authority -> authority.getAuthority())
					.sorted()
					.reduce((first, second) -> first + "," + second)
					.orElse("");
		}

		@PutMapping("/api/products/{id}")
		String updateProduct(@PathVariable Long id) {
			return "updated";
		}

		@GetMapping("/api/cart")
		String getCart() {
			return "cart";
		}

		@PostMapping("/api/cart/items")
		String addCartItem() {
			return "added";
		}

		@PutMapping("/api/cart/items/{itemId}")
		String updateCartItem(@PathVariable Long itemId) {
			return "updated";
		}

		@DeleteMapping("/api/cart/items/{itemId}")
		String deleteCartItem(@PathVariable Long itemId) {
			return "deleted";
		}

		@PostMapping("/api/cart/checkout")
		String checkout() {
			return "checked out";
		}

		@GetMapping("/api/orders")
		String getOrders() {
			return "orders";
		}

		@GetMapping("/api/orders/{orderId}")
		String getOrder(@PathVariable Long orderId) {
			return "order";
		}

		@PostMapping("/api/orders/{orderId}/cancel")
		String cancelOrder(@PathVariable Long orderId) {
			return "cancelled";
		}
	}
}
