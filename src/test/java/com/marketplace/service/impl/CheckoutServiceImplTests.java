package com.marketplace.service.impl;

import com.marketplace.entity.Cart;
import com.marketplace.entity.CustomerOrder;
import com.marketplace.entity.Product;
import com.marketplace.entity.User;
import com.marketplace.enums.OrderStatus;
import com.marketplace.enums.Role;
import com.marketplace.exception.CartEmptyException;
import com.marketplace.exception.InsufficientStockException;
import com.marketplace.exception.ProductNotFoundException;
import com.marketplace.repository.CartRepository;
import com.marketplace.repository.CustomerOrderRepository;
import com.marketplace.repository.ProductRepository;
import com.marketplace.repository.UserRepository;
import com.marketplace.service.CheckoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CheckoutServiceImplTests {

	private CartRepository cartRepository;
	private CustomerOrderRepository customerOrderRepository;
	private ProductRepository productRepository;
	private UserRepository userRepository;
	private CheckoutService checkoutService;

	@BeforeEach
	void setUp() {
		cartRepository = mock(CartRepository.class);
		customerOrderRepository = mock(CustomerOrderRepository.class);
		productRepository = mock(ProductRepository.class);
		userRepository = mock(UserRepository.class);
		checkoutService = new CheckoutServiceImpl(cartRepository, customerOrderRepository, productRepository,
				userRepository);
	}

	@Test
	void checkoutLocksProductsInIdOrderSnapshotsCurrentValuesDecrementsStockAndClearsCart() {
		User customer = activeCustomer(7L);
		Cart cart = new Cart(customer);
		cart.addItem(productReference(9L), 1);
		cart.addItem(productReference(3L), 2);
		Product currentProductThree = currentProduct(3L, "Current Three", "12.50", 5, vendor(31L));
		Product currentProductNine = currentProduct(9L, "Current Nine", "4.00", 1, vendor(91L));
		when(userRepository.findById(7L)).thenReturn(Optional.of(customer));
		when(cartRepository.findByCustomerIdForUpdate(7L)).thenReturn(Optional.of(cart));
		when(productRepository.findByIdAndActiveTrueForUpdate(3L)).thenReturn(Optional.of(currentProductThree));
		when(productRepository.findByIdAndActiveTrueForUpdate(9L)).thenReturn(Optional.of(currentProductNine));
		when(customerOrderRepository.save(org.mockito.ArgumentMatchers.any(CustomerOrder.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		CustomerOrder order = checkoutService.checkout(7L, Role.CUSTOMER);

		assertSame(customer, order.getCustomer());
		assertEquals(OrderStatus.PLACED, order.getStatus());
		assertEquals("INR", order.getCurrency());
		assertEquals(new BigDecimal("29.00"), order.getTotalAmount());
		assertEquals(2, order.getItems().size());
		assertEquals("Current Three", order.getItems().get(0).getProductName());
		assertEquals(new BigDecimal("12.50"), order.getItems().get(0).getUnitPrice());
		assertEquals(new BigDecimal("25.00"), order.getItems().get(0).getLineTotal());
		assertEquals("Current Nine", order.getItems().get(1).getProductName());
		assertEquals(new BigDecimal("4.00"), order.getItems().get(1).getUnitPrice());
		assertEquals(new BigDecimal("4.00"), order.getItems().get(1).getLineTotal());
		assertTrue(cart.getItems().isEmpty());
		verify(currentProductThree).decrementStock(2);
		verify(currentProductNine).decrementStock(1);
		verify(customerOrderRepository).save(order);
		InOrder locks = inOrder(productRepository);
		locks.verify(productRepository).findByIdAndActiveTrueForUpdate(3L);
		locks.verify(productRepository).findByIdAndActiveTrueForUpdate(9L);
	}

	@Test
	void checkoutRejectsMissingOrEmptyCart() {
		User customer = activeCustomer(7L);
		when(userRepository.findById(7L)).thenReturn(Optional.of(customer));
		when(cartRepository.findByCustomerIdForUpdate(7L)).thenReturn(Optional.empty());

		assertThrows(CartEmptyException.class, () -> checkoutService.checkout(7L, Role.CUSTOMER));

		Cart emptyCart = new Cart(customer);
		when(cartRepository.findByCustomerIdForUpdate(7L)).thenReturn(Optional.of(emptyCart));
		assertThrows(CartEmptyException.class, () -> checkoutService.checkout(7L, Role.CUSTOMER));
		verifyNoInteractions(productRepository, customerOrderRepository);
	}

	@Test
	void checkoutRejectsInactiveNonCustomerAndMismatchedActorsBeforeLoadingTheCart() {
		User inactiveCustomer = customer(7L, Role.CUSTOMER, false);
		when(userRepository.findById(7L)).thenReturn(Optional.of(inactiveCustomer));
		assertThrows(AccessDeniedException.class, () -> checkoutService.checkout(7L, Role.CUSTOMER));

		User vendor = customer(8L, Role.VENDOR, true);
		when(userRepository.findById(8L)).thenReturn(Optional.of(vendor));
		assertThrows(AccessDeniedException.class, () -> checkoutService.checkout(8L, Role.CUSTOMER));
		assertThrows(AccessDeniedException.class, () -> checkoutService.checkout(8L, Role.VENDOR));
		assertThrows(AccessDeniedException.class, () -> checkoutService.checkout(7L, Role.VENDOR));
		verifyNoInteractions(cartRepository, productRepository, customerOrderRepository);
	}

	@Test
	void checkoutRejectsMissingOrInactiveProductsWithoutPersistingOrClearingTheCart() {
		User customer = activeCustomer(7L);
		Cart cart = new Cart(customer);
		cart.addItem(productReference(3L), 1);
		when(userRepository.findById(7L)).thenReturn(Optional.of(customer));
		when(cartRepository.findByCustomerIdForUpdate(7L)).thenReturn(Optional.of(cart));
		when(productRepository.findByIdAndActiveTrueForUpdate(3L)).thenReturn(Optional.empty());

		assertThrows(ProductNotFoundException.class, () -> checkoutService.checkout(7L, Role.CUSTOMER));

		assertEquals(1, cart.getItems().size());
		verify(customerOrderRepository, never()).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void checkoutRejectsZeroOrInsufficientStockWithoutPersistingOrClearingTheCart() {
		User customer = activeCustomer(7L);
		Cart cart = new Cart(customer);
		cart.addItem(productReference(3L), 2);
		Product product = currentProduct(3L, "Current Product", "10.00", 0, vendor(31L));
		when(userRepository.findById(7L)).thenReturn(Optional.of(customer));
		when(cartRepository.findByCustomerIdForUpdate(7L)).thenReturn(Optional.of(cart));
		when(productRepository.findByIdAndActiveTrueForUpdate(3L)).thenReturn(Optional.of(product));

		assertThrows(InsufficientStockException.class, () -> checkoutService.checkout(7L, Role.CUSTOMER));

		assertEquals(1, cart.getItems().size());
		verify(product, never()).decrementStock(org.mockito.ArgumentMatchers.any());
		verify(customerOrderRepository, never()).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void checkoutPropagatesPersistenceFailuresBeforeClearingTheCart() {
		User customer = activeCustomer(7L);
		Cart cart = new Cart(customer);
		cart.addItem(productReference(3L), 1);
		Product product = currentProduct(3L, "Current Product", "10.00", 1, vendor(31L));
		when(userRepository.findById(7L)).thenReturn(Optional.of(customer));
		when(cartRepository.findByCustomerIdForUpdate(7L)).thenReturn(Optional.of(cart));
		when(productRepository.findByIdAndActiveTrueForUpdate(3L)).thenReturn(Optional.of(product));
		when(customerOrderRepository.save(org.mockito.ArgumentMatchers.any(CustomerOrder.class)))
				.thenThrow(new DataIntegrityViolationException("simulated persistence failure"));

		assertThrows(DataIntegrityViolationException.class, () -> checkoutService.checkout(7L, Role.CUSTOMER));

		assertEquals(1, cart.getItems().size());
	}

	@Test
	void checkoutIsTransactional() throws NoSuchMethodException {
		Transactional transactional = CheckoutServiceImpl.class
				.getMethod("checkout", Long.class, Role.class)
				.getAnnotation(Transactional.class);

		assertTrue(transactional != null);
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

	private User vendor(long id) {
		User vendor = mock(User.class);
		when(vendor.getId()).thenReturn(id);
		return vendor;
	}

	private Product productReference(long id) {
		Product product = mock(Product.class);
		when(product.getId()).thenReturn(id);
		return product;
	}

	private Product currentProduct(long id, String name, String price, int stock, User vendor) {
		Product product = mock(Product.class);
		when(product.getId()).thenReturn(id);
		when(product.getName()).thenReturn(name);
		when(product.getPrice()).thenReturn(new BigDecimal(price));
		when(product.getStockQuantity()).thenReturn(stock);
		when(product.getVendor()).thenReturn(vendor);
		return product;
	}
}
