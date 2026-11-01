package com.marketplace.service.impl;

import com.marketplace.entity.Cart;
import com.marketplace.entity.CartItem;
import com.marketplace.entity.Product;
import com.marketplace.entity.User;
import com.marketplace.enums.Role;
import com.marketplace.exception.CartItemNotFoundException;
import com.marketplace.exception.ProductNotFoundException;
import com.marketplace.repository.CartItemRepository;
import com.marketplace.repository.CartRepository;
import com.marketplace.repository.ProductRepository;
import com.marketplace.repository.UserRepository;
import com.marketplace.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CartServiceImplTests {

	private CartRepository cartRepository;
	private CartItemRepository cartItemRepository;
	private ProductRepository productRepository;
	private UserRepository userRepository;
	private CartService cartService;

	@BeforeEach
	void setUp() {
		cartRepository = mock(CartRepository.class);
		cartItemRepository = mock(CartItemRepository.class);
		productRepository = mock(ProductRepository.class);
		userRepository = mock(UserRepository.class);
		cartService = new CartServiceImpl(cartRepository, cartItemRepository, productRepository, userRepository);
	}

	@Test
	void activeCustomerGetsPersistedCart() {
		User customer = activeCustomer(7L);
		Cart cart = new Cart(customer);
		when(userRepository.findById(7L)).thenReturn(Optional.of(customer));
		when(cartRepository.findByCustomerId(7L)).thenReturn(Optional.of(cart));

		assertSame(cart, cartService.getCart(7L, Role.CUSTOMER));
		verify(cartRepository).findByCustomerId(7L);
	}

	@Test
	void activeCustomerGetsTransientEmptyCartWhenNoneExists() {
		User customer = activeCustomer(7L);
		when(userRepository.findById(7L)).thenReturn(Optional.of(customer));
		when(cartRepository.findByCustomerId(7L)).thenReturn(Optional.empty());

		Cart cart = cartService.getCart(7L, Role.CUSTOMER);

		assertSame(customer, cart.getCustomer());
		assertTrue(cart.getItems().isEmpty());
		verify(cartRepository, never()).save(any(Cart.class));
	}

	@Test
	void inactiveNonCustomerAndMismatchedActorsCannotAccessCarts() {
		User inactiveCustomer = customer(7L, Role.CUSTOMER, false);
		when(userRepository.findById(7L)).thenReturn(Optional.of(inactiveCustomer));
		assertThrows(AccessDeniedException.class, () -> cartService.getCart(7L, Role.CUSTOMER));

		User vendor = customer(8L, Role.VENDOR, true);
		when(userRepository.findById(8L)).thenReturn(Optional.of(vendor));
		assertThrows(AccessDeniedException.class, () -> cartService.getCart(8L, Role.CUSTOMER));
		assertThrows(AccessDeniedException.class, () -> cartService.getCart(8L, Role.VENDOR));

		verifyNoInteractions(cartRepository, cartItemRepository, productRepository);
	}

	@Test
	void addItemCreatesCartAndCartItemForActiveProduct() {
		User customer = activeCustomer(7L);
		Product product = activeProduct(3L);
		when(userRepository.findById(7L)).thenReturn(Optional.of(customer));
		when(productRepository.findByIdAndActiveTrue(3L)).thenReturn(Optional.of(product));
		when(cartRepository.findByCustomerId(7L)).thenReturn(Optional.empty());
		when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Cart cart = cartService.addItem(3L, 2, 7L, Role.CUSTOMER);

		assertSame(customer, cart.getCustomer());
		assertEquals(1, cart.getItems().size());
		assertSame(product, cart.getItems().getFirst().getProduct());
		assertEquals(2, cart.getItems().getFirst().getQuantity());
		verify(cartRepository).save(cart);
	}

	@Test
	void addItemIncreasesExistingCartItemQuantity() {
		User customer = activeCustomer(7L);
		Product product = activeProduct(3L);
		Cart cart = new Cart(customer);
		CartItem existingItem = cart.addItem(product, 2);
		when(userRepository.findById(7L)).thenReturn(Optional.of(customer));
		when(productRepository.findByIdAndActiveTrue(3L)).thenReturn(Optional.of(product));
		when(cartRepository.findByCustomerId(7L)).thenReturn(Optional.of(cart));
		when(cartRepository.save(cart)).thenReturn(cart);

		Cart result = cartService.addItem(3L, 3, 7L, Role.CUSTOMER);

		assertSame(cart, result);
		assertEquals(5, existingItem.getQuantity());
		assertEquals(1, cart.getItems().size());
		verify(cartRepository).save(cart);
	}

	@Test
	void addItemRejectsMissingOrInactiveProducts() {
		User customer = activeCustomer(7L);
		when(userRepository.findById(7L)).thenReturn(Optional.of(customer));
		when(productRepository.findByIdAndActiveTrue(3L)).thenReturn(Optional.empty());

		assertThrows(ProductNotFoundException.class, () -> cartService.addItem(3L, 1, 7L, Role.CUSTOMER));
		assertThrows(ProductNotFoundException.class, () -> cartService.addItem(0L, 1, 7L, Role.CUSTOMER));
		verifyNoInteractions(cartRepository, cartItemRepository);
	}

	@Test
	void updateAndDeleteOnlyAccessItemsOwnedByTheCustomer() {
		User customer = activeCustomer(7L);
		Product product = activeProduct(3L);
		Cart cart = new Cart(customer);
		CartItem item = cart.addItem(product, 2);
		when(userRepository.findById(7L)).thenReturn(Optional.of(customer));
		when(cartItemRepository.findByIdAndCartCustomerId(4L, 7L)).thenReturn(Optional.of(item));
		when(productRepository.findByIdAndActiveTrue(3L)).thenReturn(Optional.of(product));
		when(cartItemRepository.save(item)).thenReturn(item);

		CartItem updated = cartService.updateItem(4L, 5, 7L, Role.CUSTOMER);
		cartService.removeItem(4L, 7L, Role.CUSTOMER);

		assertSame(item, updated);
		assertEquals(5, item.getQuantity());
		verify(cartItemRepository).delete(item);

		when(cartItemRepository.findByIdAndCartCustomerId(5L, 7L)).thenReturn(Optional.empty());
		assertThrows(CartItemNotFoundException.class, () -> cartService.updateItem(5L, 1, 7L, Role.CUSTOMER));
		assertThrows(CartItemNotFoundException.class, () -> cartService.removeItem(5L, 7L, Role.CUSTOMER));
	}

	@Test
	void updateRejectsAnInactiveProductInTheCart() {
		User customer = activeCustomer(7L);
		Product product = activeProduct(3L);
		CartItem item = new Cart(customer).addItem(product, 2);
		when(userRepository.findById(7L)).thenReturn(Optional.of(customer));
		when(cartItemRepository.findByIdAndCartCustomerId(4L, 7L)).thenReturn(Optional.of(item));
		when(productRepository.findByIdAndActiveTrue(3L)).thenReturn(Optional.empty());

		assertThrows(ProductNotFoundException.class, () -> cartService.updateItem(4L, 1, 7L, Role.CUSTOMER));
		verify(cartItemRepository, never()).save(any(CartItem.class));
	}

	@Test
	void invalidAndOverflowQuantitiesAreRejected() {
		assertThrows(IllegalArgumentException.class, () -> cartService.addItem(3L, 0, 7L, Role.CUSTOMER));
		assertThrows(IllegalArgumentException.class, () -> cartService.updateItem(4L, -1, 7L, Role.CUSTOMER));
		verifyNoInteractions(userRepository, productRepository, cartRepository, cartItemRepository);

		User customer = activeCustomer(7L);
		Product product = activeProduct(3L);
		Cart cart = new Cart(customer);
		cart.addItem(product, Integer.MAX_VALUE);
		when(userRepository.findById(7L)).thenReturn(Optional.of(customer));
		when(productRepository.findByIdAndActiveTrue(3L)).thenReturn(Optional.of(product));
		when(cartRepository.findByCustomerId(7L)).thenReturn(Optional.of(cart));

		assertThrows(IllegalArgumentException.class, () -> cartService.addItem(3L, 1, 7L, Role.CUSTOMER));
		verify(cartRepository, never()).save(any(Cart.class));
	}

	private User activeCustomer(long id) {
		return customer(id, Role.CUSTOMER, true);
	}

	private User customer(long id, Role role, boolean active) {
		User user = mock(User.class);
		when(user.getId()).thenReturn(id);
		when(user.getRole()).thenReturn(role);
		when(user.isActive()).thenReturn(active);
		return user;
	}

	private Product activeProduct(long id) {
		Product product = mock(Product.class);
		when(product.getId()).thenReturn(id);
		when(product.getName()).thenReturn("Product");
		when(product.getPrice()).thenReturn(new BigDecimal("10.00"));
		return product;
	}
}
