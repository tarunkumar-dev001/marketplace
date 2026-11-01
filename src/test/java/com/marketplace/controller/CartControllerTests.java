package com.marketplace.controller;

import com.marketplace.entity.Cart;
import com.marketplace.entity.CartItem;
import com.marketplace.entity.CustomerOrder;
import com.marketplace.entity.OrderItem;
import com.marketplace.entity.Product;
import com.marketplace.enums.OrderStatus;
import com.marketplace.enums.Role;
import com.marketplace.exception.CartEmptyException;
import com.marketplace.exception.CartItemNotFoundException;
import com.marketplace.exception.GlobalExceptionHandler;
import com.marketplace.exception.InsufficientStockException;
import com.marketplace.service.CartService;
import com.marketplace.service.CheckoutService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

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

class CartControllerTests {

	private static final String ADD_REQUEST = """
			{
			  "productId": 3,
			  "quantity": 2
			}
			""";
	private static final String QUANTITY_REQUEST = """
			{
			  "quantity": 5
			}
			""";

	private CartService cartService;
	private CheckoutService checkoutService;
	private LocalValidatorFactoryBean validator;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		cartService = mock(CartService.class);
		checkoutService = mock(CheckoutService.class);
		validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mockMvc = MockMvcBuilders.standaloneSetup(new CartController(cartService, checkoutService))
				.setControllerAdvice(new GlobalExceptionHandler())
				.setValidator(validator)
				.build();
	}

	@AfterEach
	void tearDown() {
		validator.destroy();
	}

	@Test
	void getCartReturnsTheCurrentCartEnvelope() throws Exception {
		Cart cart = cart();
		when(cartService.getCart(7L, Role.CUSTOMER)).thenReturn(cart);

		mockMvc.perform(get("/api/cart").principal(principal()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Cart retrieved"))
				.andExpect(jsonPath("$.data.id").value(1))
				.andExpect(jsonPath("$.data.items[0].id").value(2))
				.andExpect(jsonPath("$.data.items[0].productId").value(3))
				.andExpect(jsonPath("$.data.items[0].productName").value("Wireless Mouse"))
				.andExpect(jsonPath("$.data.items[0].price").value(10.50))
				.andExpect(jsonPath("$.data.items[0].lineTotal").value(21.00))
				.andExpect(jsonPath("$.data.subtotal").value(21.00))
				.andExpect(jsonPath("$.data.updatedAt").value("2026-01-15T10:15:30Z"));

		verify(cartService).getCart(7L, Role.CUSTOMER);
	}

	@Test
	void getCartReturnsAnEmptyTransientCartWithoutAStoredId() throws Exception {
		Cart emptyCart = new Cart(mock(com.marketplace.entity.User.class));
		when(cartService.getCart(7L, Role.CUSTOMER)).thenReturn(emptyCart);

		mockMvc.perform(get("/api/cart").principal(principal()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value((Object) null))
				.andExpect(jsonPath("$.data.items").isEmpty())
				.andExpect(jsonPath("$.data.subtotal").value(0));
	}

	@Test
	void addItemReturnsUpdatedCartAndDelegatesToService() throws Exception {
		Cart cart = cart();
		when(cartService.addItem(3L, 2, 7L, Role.CUSTOMER)).thenReturn(cart);

		mockMvc.perform(post("/api/cart/items").contentType(MediaType.APPLICATION_JSON)
					.principal(principal()).content(ADD_REQUEST))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Item added to cart"))
				.andExpect(jsonPath("$.data.items[0].quantity").value(2));

		verify(cartService).addItem(3L, 2, 7L, Role.CUSTOMER);
	}

	@Test
	void updateItemReturnsUpdatedItemAndDelegatesToService() throws Exception {
		CartItem item = cartItem(5);
		when(cartService.updateItem(2L, 5, 7L, Role.CUSTOMER)).thenReturn(item);

		mockMvc.perform(put("/api/cart/items/{itemId}", 2L).contentType(MediaType.APPLICATION_JSON)
					.principal(principal()).content(QUANTITY_REQUEST))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Cart item updated"))
				.andExpect(jsonPath("$.data.quantity").value(5))
				.andExpect(jsonPath("$.data.lineTotal").value(52.50));

		verify(cartService).updateItem(2L, 5, 7L, Role.CUSTOMER);
	}

	@Test
	void removeItemReturnsNoContentAndDelegatesToService() throws Exception {
		doNothing().when(cartService).removeItem(2L, 7L, Role.CUSTOMER);

		mockMvc.perform(delete("/api/cart/items/{itemId}", 2L).principal(principal()))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		verify(cartService).removeItem(2L, 7L, Role.CUSTOMER);
	}

	@Test
	void invalidItemRequestsReturnValidationErrorsWithoutCallingTheService() throws Exception {
		assertInvalid(post("/api/cart/items"), """
				{
				  "productId": 0,
				  "quantity": 0
				}
				""");
		assertInvalid(put("/api/cart/items/{itemId}", 2L), """
				{
				  "quantity": 0
				}
				""");

		verifyNoInteractions(cartService);
	}

	@Test
	void cartItemNotFoundUsesTheStandardNotFoundResponse() throws Exception {
		when(cartService.updateItem(99L, 5, 7L, Role.CUSTOMER)).thenThrow(new CartItemNotFoundException());

		mockMvc.perform(put("/api/cart/items/{itemId}", 99L).contentType(MediaType.APPLICATION_JSON)
					.principal(principal()).content(QUANTITY_REQUEST))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Cart item not found"))
				.andExpect(jsonPath("$.data").value((Object) null));
	}

	@Test
	void checkoutReturnsCreatedOrderEnvelopeAndDelegatesToTheService() throws Exception {
		CustomerOrder order = checkoutOrder();
		when(checkoutService.checkout(7L, Role.CUSTOMER)).thenReturn(order);

		mockMvc.perform(post("/api/cart/checkout").principal(principal()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Checkout completed"))
				.andExpect(jsonPath("$.data.orderId").value(10))
				.andExpect(jsonPath("$.data.status").value("PLACED"))
				.andExpect(jsonPath("$.data.currency").value("INR"))
				.andExpect(jsonPath("$.data.totalAmount").value(21.00))
				.andExpect(jsonPath("$.data.createdAt").value("2026-01-15T10:15:30Z"))
				.andExpect(jsonPath("$.data.items[0].productId").value(3))
				.andExpect(jsonPath("$.data.items[0].productName").value("Purchased Mouse"))
				.andExpect(jsonPath("$.data.items[0].unitPrice").value(10.50))
				.andExpect(jsonPath("$.data.items[0].quantity").value(2))
				.andExpect(jsonPath("$.data.items[0].lineTotal").value(21.00));

		verify(checkoutService).checkout(7L, Role.CUSTOMER);
	}

	@Test
	void checkoutMapsEmptyCartAndInsufficientStockToClientErrors() throws Exception {
		when(checkoutService.checkout(7L, Role.CUSTOMER))
				.thenThrow(new CartEmptyException(), new InsufficientStockException());

		mockMvc.perform(post("/api/cart/checkout").principal(principal()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Cart is empty"));

		mockMvc.perform(post("/api/cart/checkout").principal(principal()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Insufficient stock"));
	}

	private void assertInvalid(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
			String body) throws Exception {
		mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).principal(principal()).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Validation failed"));
	}

	private Cart cart() {
		Cart cart = mock(Cart.class);
		CartItem item = cartItem(2);
		when(cart.getId()).thenReturn(1L);
		when(cart.getItems()).thenReturn(List.of(item));
		when(cart.getUpdatedAt()).thenReturn(Instant.parse("2026-01-15T10:15:30Z"));
		return cart;
	}

	private CartItem cartItem(int quantity) {
		Product product = mock(Product.class);
		when(product.getId()).thenReturn(3L);
		when(product.getName()).thenReturn("Wireless Mouse");
		when(product.getPrice()).thenReturn(new BigDecimal("10.50"));
		CartItem item = mock(CartItem.class);
		when(item.getId()).thenReturn(2L);
		when(item.getProduct()).thenReturn(product);
		when(item.getQuantity()).thenReturn(quantity);
		return item;
	}

	private CustomerOrder checkoutOrder() {
		Product product = mock(Product.class);
		when(product.getId()).thenReturn(3L);
		OrderItem item = mock(OrderItem.class);
		when(item.getProduct()).thenReturn(product);
		when(item.getProductName()).thenReturn("Purchased Mouse");
		when(item.getUnitPrice()).thenReturn(new BigDecimal("10.50"));
		when(item.getQuantity()).thenReturn(2);
		when(item.getLineTotal()).thenReturn(new BigDecimal("21.00"));
		CustomerOrder order = mock(CustomerOrder.class);
		when(order.getId()).thenReturn(10L);
		when(order.getStatus()).thenReturn(OrderStatus.PLACED);
		when(order.getCurrency()).thenReturn("INR");
		when(order.getTotalAmount()).thenReturn(new BigDecimal("21.00"));
		when(order.getCreatedAt()).thenReturn(Instant.parse("2026-01-15T10:15:30Z"));
		when(order.getItems()).thenReturn(List.of(item));
		return order;
	}

	private JwtAuthenticationToken principal() {
		Jwt jwt = Jwt.withTokenValue("test-token")
				.header("alg", "none")
				.subject("7")
				.claim("role", Role.CUSTOMER.name())
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(60))
				.build();
		return new JwtAuthenticationToken(jwt);
	}
}
