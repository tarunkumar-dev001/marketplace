package com.marketplace.service.impl;

import com.marketplace.entity.CustomerOrder;
import com.marketplace.entity.OrderItem;
import com.marketplace.entity.Product;
import com.marketplace.entity.User;
import com.marketplace.enums.OrderStatus;
import com.marketplace.enums.Role;
import com.marketplace.exception.OrderCancellationNotAllowedException;
import com.marketplace.exception.OrderNotFoundException;
import com.marketplace.repository.CustomerOrderRepository;
import com.marketplace.repository.ProductRepository;
import com.marketplace.repository.UserRepository;
import com.marketplace.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OrderServiceImplTests {

	private CustomerOrderRepository customerOrderRepository;
	private ProductRepository productRepository;
	private UserRepository userRepository;
	private OrderService orderService;

	@BeforeEach
	void setUp() {
		customerOrderRepository = mock(CustomerOrderRepository.class);
		productRepository = mock(ProductRepository.class);
		userRepository = mock(UserRepository.class);
		orderService = new OrderServiceImpl(customerOrderRepository, productRepository, userRepository);
	}

	@Test
	void historyIsScopedToThePersistedCustomerAndRetainsPageOrdering() {
		User customer = activeCustomer(7L);
		CustomerOrder first = order(11L);
		CustomerOrder second = order(10L);
		Pageable pageable = PageRequest.of(0, 2);
		when(userRepository.findById(7L)).thenReturn(Optional.of(customer));
		when(customerOrderRepository.findByCustomerId(7L, pageable))
				.thenReturn(new PageImpl<>(List.of(first, second), pageable, 4));
		when(customerOrderRepository.findByCustomerIdAndIdInWithItems(7L, List.of(11L, 10L)))
				.thenReturn(List.of(second, first));

		Page<CustomerOrder> result = orderService.getOrders(7L, Role.CUSTOMER, pageable);

		assertEquals(List.of(first, second), result.getContent());
		assertEquals(4, result.getTotalElements());
		verify(customerOrderRepository).findByCustomerId(7L, pageable);
		verify(customerOrderRepository).findByCustomerIdAndIdInWithItems(7L, List.of(11L, 10L));
	}

	@Test
	void emptyHistoryDoesNotIssueAnItemFetch() {
		User customer = activeCustomer(7L);
		Pageable pageable = PageRequest.of(0, 20);
		when(userRepository.findById(7L)).thenReturn(Optional.of(customer));
		when(customerOrderRepository.findByCustomerId(7L, pageable)).thenReturn(Page.empty(pageable));

		assertTrue(orderService.getOrders(7L, Role.CUSTOMER, pageable).isEmpty());

		verify(customerOrderRepository, never()).findByCustomerIdAndIdInWithItems(
				org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyList());
	}

	@Test
	void detailLookupReturnsOnlyAnOwnedOrderAndHidesMissingOrForeignOrders() {
		User customer = activeCustomer(7L);
		CustomerOrder order = order(11L);
		when(userRepository.findById(7L)).thenReturn(Optional.of(customer));
		when(customerOrderRepository.findByIdAndCustomerId(11L, 7L)).thenReturn(Optional.of(order));

		assertSame(order, orderService.getOrder(11L, 7L, Role.CUSTOMER));

		when(customerOrderRepository.findByIdAndCustomerId(12L, 7L)).thenReturn(Optional.empty());
		assertThrows(OrderNotFoundException.class, () -> orderService.getOrder(12L, 7L, Role.CUSTOMER));
		assertThrows(OrderNotFoundException.class, () -> orderService.getOrder(0L, 7L, Role.CUSTOMER));
	}

	@Test
	void inactiveNonCustomerAndMismatchedActorsCannotReadOrders() {
		User inactiveCustomer = customer(7L, Role.CUSTOMER, false);
		when(userRepository.findById(7L)).thenReturn(Optional.of(inactiveCustomer));
		assertThrows(AccessDeniedException.class, () -> orderService.getOrders(7L, Role.CUSTOMER, PageRequest.of(0, 20)));
		assertThrows(AccessDeniedException.class, () -> orderService.cancelOrder(1L, 7L, Role.CUSTOMER));

		User vendor = customer(8L, Role.VENDOR, true);
		when(userRepository.findById(8L)).thenReturn(Optional.of(vendor));
		assertThrows(AccessDeniedException.class, () -> orderService.getOrder(1L, 8L, Role.CUSTOMER));
		assertThrows(AccessDeniedException.class, () -> orderService.getOrder(1L, 8L, Role.VENDOR));
		assertThrows(AccessDeniedException.class, () -> orderService.cancelOrder(1L, 8L, Role.CUSTOMER));
		verifyNoInteractions(customerOrderRepository);
	}

	@Test
	void cancellationLocksProductsInIdOrderRestoresRecordedQuantitiesAndPersistsTheOrder() {
		User customer = activeCustomer(7L);
		CustomerOrder order = mock(CustomerOrder.class);
		OrderItem itemNine = orderItem(9L, 1);
		OrderItem itemThree = orderItem(3L, 2);
		Product productThree = mock(Product.class);
		Product productNine = mock(Product.class);
		when(userRepository.findById(7L)).thenReturn(Optional.of(customer));
		when(customerOrderRepository.findByIdAndCustomerIdForUpdate(11L, 7L)).thenReturn(Optional.of(order));
		when(order.getItems()).thenReturn(List.of(itemNine, itemThree));
		when(productRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(productThree));
		when(productRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(productNine));
		when(customerOrderRepository.save(order)).thenReturn(order);

		assertSame(order, orderService.cancelOrder(11L, 7L, Role.CUSTOMER));

		verify(order).cancel();
		verify(productThree).restoreStock(2);
		verify(productNine).restoreStock(1);
		verify(customerOrderRepository).save(order);
		var locks = inOrder(productRepository);
		locks.verify(productRepository).findByIdForUpdate(3L);
		locks.verify(productRepository).findByIdForUpdate(9L);
	}

	@Test
	void cancellationHidesMissingOrForeignOrdersAndRejectsNonPlacedStatuses() {
		User customer = activeCustomer(7L);
		when(userRepository.findById(7L)).thenReturn(Optional.of(customer));
		when(customerOrderRepository.findByIdAndCustomerIdForUpdate(11L, 7L)).thenReturn(Optional.empty());
		assertThrows(OrderNotFoundException.class, () -> orderService.cancelOrder(11L, 7L, Role.CUSTOMER));

		CustomerOrder cancelled = new CustomerOrder(customer, OrderStatus.CANCELLED, "INR", java.math.BigDecimal.ONE);
		when(customerOrderRepository.findByIdAndCustomerIdForUpdate(12L, 7L)).thenReturn(Optional.of(cancelled));
		assertThrows(OrderCancellationNotAllowedException.class,
				() -> orderService.cancelOrder(12L, 7L, Role.CUSTOMER));

		CustomerOrder fulfilled = new CustomerOrder(customer, OrderStatus.FULFILLED, "INR", java.math.BigDecimal.ONE);
		when(customerOrderRepository.findByIdAndCustomerIdForUpdate(13L, 7L)).thenReturn(Optional.of(fulfilled));
		assertThrows(OrderCancellationNotAllowedException.class,
				() -> orderService.cancelOrder(13L, 7L, Role.CUSTOMER));
		verifyNoInteractions(productRepository);
	}

	@Test
	void cancellationPropagatesPersistenceFailuresWithoutSuppressingThem() {
		User customer = activeCustomer(7L);
		CustomerOrder order = mock(CustomerOrder.class);
		OrderItem item = orderItem(3L, 2);
		Product product = mock(Product.class);
		when(userRepository.findById(7L)).thenReturn(Optional.of(customer));
		when(customerOrderRepository.findByIdAndCustomerIdForUpdate(11L, 7L)).thenReturn(Optional.of(order));
		when(order.getItems()).thenReturn(List.of(item));
		when(productRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(product));
		when(customerOrderRepository.save(order)).thenThrow(new DataIntegrityViolationException("simulated failure"));

		assertThrows(DataIntegrityViolationException.class, () -> orderService.cancelOrder(11L, 7L, Role.CUSTOMER));

		verify(product).restoreStock(2);
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

	private CustomerOrder order(long id) {
		CustomerOrder order = mock(CustomerOrder.class);
		when(order.getId()).thenReturn(id);
		return order;
	}

	private OrderItem orderItem(long productId, int quantity) {
		Product product = mock(Product.class);
		when(product.getId()).thenReturn(productId);
		OrderItem item = mock(OrderItem.class);
		when(item.getProduct()).thenReturn(product);
		when(item.getQuantity()).thenReturn(quantity);
		return item;
	}
}
