package com.marketplace.security;

import com.marketplace.controller.ProductController;
import com.marketplace.entity.Product;
import com.marketplace.entity.User;
import com.marketplace.enums.Role;
import com.marketplace.exception.GlobalExceptionHandler;
import com.marketplace.repository.ProductRepository;
import com.marketplace.repository.UserRepository;
import com.marketplace.service.ProductService;
import com.marketplace.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductAuthorizationIntegrationTests {

	private static final String ISSUER = "https://marketplace-product-authorization.test";
	private static final String PRODUCT_REQUEST = """
			{
			  "name": "Updated Product",
			  "description": "Updated description",
			  "price": 15.00,
			  "stockQuantity": 2,
			  "category": "Books",
			  "active": true
			}
			""";

	private AnnotationConfigWebApplicationContext context;
	private MockMvc mockMvc;
	private JwtEncoder jwtEncoder;
	private ProductRepository productRepository;
	private UserRepository userRepository;

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
		productRepository = context.getBean(ProductRepository.class);
		userRepository = context.getBean(UserRepository.class);
		mockMvc = MockMvcBuilders.webAppContextSetup(context)
				.apply(SecurityMockMvcConfigurers.springSecurity())
				.build();
	}

	@AfterEach
	void tearDown() {
		context.close();
	}

	@Test
	void validVendorJwtCannotUpdateOrDeleteAnotherVendorsProduct() throws Exception {
		User actingVendor = user(7L, Role.VENDOR, true);
		User owner = user(8L, Role.VENDOR, true);
		Product otherVendorProduct = new Product(
				"Original", "Description", new BigDecimal("10.00"), 3, "Books", true, owner);
		when(userRepository.findById(7L)).thenReturn(Optional.of(actingVendor));
		when(productRepository.findById(1L)).thenReturn(Optional.of(otherVendorProduct));
		String bearerToken = "Bearer " + token("7", "VENDOR");

		mockMvc.perform(put("/api/products/{id}", 1L)
					.header("Authorization", bearerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(PRODUCT_REQUEST))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Access denied"));
		mockMvc.perform(delete("/api/products/{id}", 1L)
					.header("Authorization", bearerToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Access denied"));

		verify(productRepository, never()).save(any(Product.class));
		verify(productRepository, never()).delete(any(Product.class));
	}

	private String token(String subject, String role) {
		Instant issuedAt = Instant.now().minusSeconds(1);
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(ISSUER)
				.subject(subject)
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plusSeconds(900))
				.claim("role", role)
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	private User user(long id, Role role, boolean active) {
		User user = mock(User.class);
		when(user.getId()).thenReturn(id);
		when(user.getRole()).thenReturn(role);
		when(user.isActive()).thenReturn(active);
		return user;
	}

	@Configuration
	@EnableWebMvc
	static class TestWebConfig {

		@Bean
		ProductRepository productRepository() {
			return mock(ProductRepository.class);
		}

		@Bean
		UserRepository userRepository() {
			return mock(UserRepository.class);
		}

		@Bean
		ProductService productService(ProductRepository productRepository, UserRepository userRepository) {
			return new ProductServiceImpl(productRepository, userRepository);
		}

		@Bean
		ProductController productController(ProductService productService) {
			return new ProductController(productService);
		}

		@Bean
		GlobalExceptionHandler globalExceptionHandler() {
			return new GlobalExceptionHandler();
		}
	}
}
