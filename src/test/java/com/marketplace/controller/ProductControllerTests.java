package com.marketplace.controller;

import com.marketplace.entity.Product;
import com.marketplace.enums.Role;
import com.marketplace.exception.GlobalExceptionHandler;
import com.marketplace.exception.ProductNotFoundException;
import com.marketplace.service.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerTests {

	private static final Instant CREATED_AT = Instant.parse("2026-01-15T10:15:30Z");
	private static final Instant UPDATED_AT = Instant.parse("2026-01-16T11:20:45Z");
	private static final String VALID_REQUEST = """
			{
			  "name": "Wireless Headphones",
			  "description": "Noise-cancelling headphones",
			  "price": 129.99,
			  "stockQuantity": 25,
			  "category": "Electronics",
			  "active": true
			}
			""";

	private ProductService productService;
	private LocalValidatorFactoryBean validator;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		productService = mock(ProductService.class);
		validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mockMvc = MockMvcBuilders.standaloneSetup(new ProductController(productService))
				.setControllerAdvice(new GlobalExceptionHandler())
				.setValidator(validator)
				.build();
	}

	@AfterEach
	void tearDown() {
		validator.destroy();
	}

	@Test
	void createReturnsCreatedResponseAndDelegatesToService() throws Exception {
		Product savedProduct = product(1L, "Wireless Headphones", true);
		when(productService.create(any(Product.class), any(), any())).thenReturn(savedProduct);

		mockMvc.perform(post("/api/products")
					.contentType(MediaType.APPLICATION_JSON)
					.principal(principal(Role.ADMIN))
					.content(VALID_REQUEST))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Product created"))
				.andExpect(jsonPath("$.data.id").value(1))
				.andExpect(jsonPath("$.data.name").value("Wireless Headphones"))
				.andExpect(jsonPath("$.data.createdAt").value(CREATED_AT.toString()));

		ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
		verify(productService).create(productCaptor.capture(), eq(1L), eq(Role.ADMIN));
		assertProductMatchesRequest(productCaptor.getValue());
	}

	@Test
	void createDefaultsOmittedActiveToTrueAndPreservesExplicitFalse() throws Exception {
		Product savedProduct = product(1L, "Wireless Headphones", true);
		when(productService.create(any(Product.class), any(), any())).thenReturn(savedProduct);

		mockMvc.perform(post("/api/products")
					.contentType(MediaType.APPLICATION_JSON)
					.principal(principal(Role.ADMIN))
					.content(VALID_REQUEST.replace(",\n  \"active\": true", "")))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/products")
					.contentType(MediaType.APPLICATION_JSON)
					.principal(principal(Role.ADMIN))
					.content(VALID_REQUEST.replace("true", "false")))
				.andExpect(status().isCreated());

		ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
		verify(productService, org.mockito.Mockito.times(2)).create(productCaptor.capture(), eq(1L), eq(Role.ADMIN));
		assertTrue(productCaptor.getAllValues().get(0).isActive());
		assertEquals(false, productCaptor.getAllValues().get(1).isActive());
	}

	@Test
	void createAndUpdateTrimNameAndCategory() throws Exception {
		Product savedProduct = product(1L, "Wireless Headphones", true);
		when(productService.create(any(Product.class), any(), any())).thenReturn(savedProduct);
		when(productService.update(eq(1L), any(Product.class), any(), any())).thenReturn(savedProduct);
		String request = VALID_REQUEST.replace("Wireless Headphones", "  Wireless Headphones  ")
				.replace("Electronics", "  Electronics  ");

		mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
					.principal(principal(Role.ADMIN)).content(request))
				.andExpect(status().isCreated());
		mockMvc.perform(put("/api/products/{id}", 1L).contentType(MediaType.APPLICATION_JSON)
					.principal(principal(Role.ADMIN)).content(request))
				.andExpect(status().isOk());

		ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
		verify(productService).create(productCaptor.capture(), eq(1L), eq(Role.ADMIN));
		assertEquals("Wireless Headphones", productCaptor.getValue().getName());
		assertEquals("Electronics", productCaptor.getValue().getCategory());
		verify(productService).update(eq(1L), productCaptor.capture(), eq(1L), eq(Role.ADMIN));
		assertEquals("Wireless Headphones", productCaptor.getValue().getName());
		assertEquals("Electronics", productCaptor.getValue().getCategory());
	}

	@Test
	void getAllReturnsPaginatedProductResponses() throws Exception {
		Product product = product(1L, "Wireless Headphones", true);
		when(productService.getAll(any(Pageable.class), any(), any()))
				.thenReturn(new PageImpl<>(List.of(product), PageRequest.of(0, 20), 1));

		mockMvc.perform(get("/api/products").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Products retrieved"))
				.andExpect(jsonPath("$.data.items[0].id").value(1))
				.andExpect(jsonPath("$.data.items[0].category").value("Electronics"))
				.andExpect(jsonPath("$.data.items[0].updatedAt").value(UPDATED_AT.toString()))
				.andExpect(jsonPath("$.data.page").value(0))
				.andExpect(jsonPath("$.data.size").value(20))
				.andExpect(jsonPath("$.data.totalElements").value(1));

		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(productService).getAll(pageableCaptor.capture(), org.mockito.ArgumentMatchers.isNull(),
				org.mockito.ArgumentMatchers.isNull());
		assertEquals(Sort.Direction.DESC, pageableCaptor.getValue().getSort().getOrderFor("createdAt").getDirection());
		assertEquals(Sort.Direction.DESC, pageableCaptor.getValue().getSort().getOrderFor("id").getDirection());
	}

	@Test
	void getAllSupportsPaginationSortingCategoryAndNameSearch() throws Exception {
		when(productService.getAll(any(Pageable.class), any(), any()))
				.thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 10), 0));

		mockMvc.perform(get("/api/products")
					.param("page", "1")
					.param("size", "10")
					.param("category", "  Electronics  ")
					.param("search", "  headphones  ")
					.param("sortBy", "price")
					.param("direction", "asc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items").isEmpty())
				.andExpect(jsonPath("$.data.page").value(1));

		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(productService).getAll(pageableCaptor.capture(), eq("Electronics"), eq("headphones"));
		assertEquals(1, pageableCaptor.getValue().getPageNumber());
		assertEquals(10, pageableCaptor.getValue().getPageSize());
		assertEquals(Sort.Direction.ASC, pageableCaptor.getValue().getSort().getOrderFor("price").getDirection());
		assertEquals(Sort.Direction.ASC, pageableCaptor.getValue().getSort().getOrderFor("id").getDirection());
	}

	@Test
	void getMineReturnsVendorProductsIncludingInactiveProducts() throws Exception {
		Product inactiveProduct = product(1L, "Archived Headphones", false);
		when(productService.getMine(any(Pageable.class), eq(1L), eq(Role.VENDOR)))
				.thenReturn(new PageImpl<>(List.of(inactiveProduct), PageRequest.of(0, 20), 1));

		mockMvc.perform(get("/api/products/mine").principal(principal(Role.VENDOR)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Vendor products retrieved"))
				.andExpect(jsonPath("$.data.items[0].id").value(1))
				.andExpect(jsonPath("$.data.items[0].active").value(false));

		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(productService).getMine(pageableCaptor.capture(), eq(1L), eq(Role.VENDOR));
		assertEquals(Sort.Direction.DESC, pageableCaptor.getValue().getSort().getOrderFor("createdAt").getDirection());
		assertEquals(Sort.Direction.DESC, pageableCaptor.getValue().getSort().getOrderFor("id").getDirection());
	}

	@Test
	void getByIdReturnsProductResponse() throws Exception {
		Product product = product(1L, "Wireless Headphones", true);
		when(productService.getById(1L)).thenReturn(product);

		mockMvc.perform(get("/api/products/{id}", 1L).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Product retrieved"))
				.andExpect(jsonPath("$.data.id").value(1))
				.andExpect(jsonPath("$.data.price").value(129.99));

		verify(productService).getById(1L);
	}

	@Test
	void getByIdReturnsNotFoundResponseWhenProductIsMissing() throws Exception {
		when(productService.getById(99L)).thenThrow(new ProductNotFoundException());

		mockMvc.perform(get("/api/products/{id}", 99L).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Product not found"))
				.andExpect(jsonPath("$.data").value((Object) null));

		verify(productService).getById(99L);
	}

	@Test
	void updateReturnsUpdatedResponseAndDelegatesToService() throws Exception {
		Product updatedProduct = product(1L, "Wireless Headphones", true);
		when(productService.update(eq(1L), any(Product.class), any(), any())).thenReturn(updatedProduct);

		mockMvc.perform(put("/api/products/{id}", 1L)
					.contentType(MediaType.APPLICATION_JSON)
					.principal(principal(Role.ADMIN))
					.content(VALID_REQUEST))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Product updated"))
				.andExpect(jsonPath("$.data.id").value(1))
				.andExpect(jsonPath("$.data.active").value(true));

		ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
		verify(productService).update(eq(1L), productCaptor.capture(), eq(1L), eq(Role.ADMIN));
		assertProductMatchesRequest(productCaptor.getValue());
	}

	@Test
	void deleteReturnsNoContentAndDelegatesToService() throws Exception {
		doNothing().when(productService).delete(1L, 1L, Role.ADMIN);

		mockMvc.perform(delete("/api/products/{id}", 1L).principal(principal(Role.ADMIN)))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		verify(productService).delete(1L, 1L, Role.ADMIN);
	}

	@Test
	void invalidRequestsReturnBadRequestValidationErrorResponse() throws Exception {
		assertInvalidRequest("""
				{
				  "description": "Description",
				  "price": 10.00,
				  "stockQuantity": 1,
				  "category": "Electronics",
				  "active": true
				}
				""");
		assertInvalidRequest(VALID_REQUEST.replace("Wireless Headphones", " "));
		assertInvalidRequest(VALID_REQUEST.replace("129.99", "0.00"));
		assertInvalidRequest(VALID_REQUEST.replace("129.99", "1.001"));
		assertInvalidRequest(VALID_REQUEST.replace("129.99", "100000000000000000.00"));
		assertInvalidRequest(VALID_REQUEST.replace("25", "-1"));
		assertInvalidRequest(VALID_REQUEST.replace("Electronics", ""));

		verifyNoInteractions(productService);
	}

	private void assertInvalidRequest(String requestBody) throws Exception {
		mockMvc.perform(post("/api/products")
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.data").value((Object) null));
	}

	private Product product(Long id, String name, boolean active) {
		Product product = mock(Product.class);
		when(product.getId()).thenReturn(id);
		when(product.getName()).thenReturn(name);
		when(product.getDescription()).thenReturn("Noise-cancelling headphones");
		when(product.getPrice()).thenReturn(new BigDecimal("129.99"));
		when(product.getStockQuantity()).thenReturn(25);
		when(product.getCategory()).thenReturn("Electronics");
		when(product.isActive()).thenReturn(active);
		when(product.getCreatedAt()).thenReturn(CREATED_AT);
		when(product.getUpdatedAt()).thenReturn(UPDATED_AT);
		return product;
	}

	private void assertProductMatchesRequest(Product product) {
		assertEquals("Wireless Headphones", product.getName());
		assertEquals("Noise-cancelling headphones", product.getDescription());
		assertEquals(new BigDecimal("129.99"), product.getPrice());
		assertEquals(25, product.getStockQuantity());
		assertEquals("Electronics", product.getCategory());
		assertTrue(product.isActive());
	}

	private JwtAuthenticationToken principal(Role role) {
		Jwt jwt = Jwt.withTokenValue("test-token")
				.header("alg", "none")
				.subject("1")
				.claim("role", role.name())
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(60))
				.build();
		return new JwtAuthenticationToken(jwt);
	}
}
