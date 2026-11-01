package com.marketplace.security;

import com.marketplace.controller.CartController;
import com.marketplace.entity.User;
import com.marketplace.enums.Role;
import com.marketplace.exception.GlobalExceptionHandler;
import com.marketplace.repository.CartItemRepository;
import com.marketplace.repository.CartRepository;
import com.marketplace.repository.ProductRepository;
import com.marketplace.repository.UserRepository;
import com.marketplace.service.CartService;
import com.marketplace.service.CheckoutService;
import com.marketplace.service.impl.CartServiceImpl;
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

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CartAuthorizationIntegrationTests {

	private static final String ISSUER = "https://marketplace-cart-authorization.test";
	private static final String QUANTITY_REQUEST = """
			{
			  "quantity": 2
			}
			""";

	private AnnotationConfigWebApplicationContext context;
	private MockMvc mockMvc;
	private JwtEncoder jwtEncoder;
	private CartItemRepository cartItemRepository;
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
		cartItemRepository = context.getBean(CartItemRepository.class);
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
	void validCustomerJwtCannotUpdateOrDeleteAnotherCustomersCartItem() throws Exception {
		User customer = customer(7L);
		when(userRepository.findById(7L)).thenReturn(Optional.of(customer));
		when(cartItemRepository.findByIdAndCartCustomerId(1L, 7L)).thenReturn(Optional.empty());
		String bearerToken = "Bearer " + token("7", "CUSTOMER");

		mockMvc.perform(put("/api/cart/items/{itemId}", 1L)
					.header("Authorization", bearerToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(QUANTITY_REQUEST))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Cart item not found"));
		mockMvc.perform(delete("/api/cart/items/{itemId}", 1L)
					.header("Authorization", bearerToken))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Cart item not found"));

		verify(cartItemRepository, never()).delete(org.mockito.ArgumentMatchers.any());
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

	private User customer(long id) {
		User user = mock(User.class);
		when(user.getId()).thenReturn(id);
		when(user.getRole()).thenReturn(Role.CUSTOMER);
		when(user.isActive()).thenReturn(true);
		return user;
	}

	@Configuration
	@EnableWebMvc
	static class TestWebConfig {

		@Bean
		CartRepository cartRepository() {
			return mock(CartRepository.class);
		}

		@Bean
		CartItemRepository cartItemRepository() {
			return mock(CartItemRepository.class);
		}

		@Bean
		ProductRepository productRepository() {
			return mock(ProductRepository.class);
		}

		@Bean
		UserRepository userRepository() {
			return mock(UserRepository.class);
		}

		@Bean
		CartService cartService(CartRepository cartRepository, CartItemRepository cartItemRepository,
				ProductRepository productRepository, UserRepository userRepository) {
			return new CartServiceImpl(cartRepository, cartItemRepository, productRepository, userRepository);
		}

		@Bean
		CheckoutService checkoutService() {
			return mock(CheckoutService.class);
		}

		@Bean
		CartController cartController(CartService cartService, CheckoutService checkoutService) {
			return new CartController(cartService, checkoutService);
		}

		@Bean
		GlobalExceptionHandler globalExceptionHandler() {
			return new GlobalExceptionHandler();
		}
	}
}
