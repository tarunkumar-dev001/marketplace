package com.marketplace.service.impl;

import com.marketplace.entity.CustomerOrder;
import com.marketplace.entity.OrderItem;
import com.marketplace.entity.Product;
import com.marketplace.entity.User;
import com.marketplace.enums.Role;
import com.marketplace.enums.OrderStatus;
import com.marketplace.enums.FulfillmentStatus;
import com.marketplace.dto.AdminOrderStatisticsResponse;
import com.marketplace.dto.VendorOrderStatisticsResponse;
import com.marketplace.exception.OrderNotFoundException;
import com.marketplace.exception.ProductNotFoundException;
import com.marketplace.repository.CustomerOrderRepository;
import com.marketplace.repository.ProductRepository;
import com.marketplace.repository.UserRepository;
import com.marketplace.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.math.BigDecimal;

@Service
public class OrderServiceImpl implements OrderService {

	private final CustomerOrderRepository customerOrderRepository;
	private final ProductRepository productRepository;
	private final UserRepository userRepository;

	public OrderServiceImpl(CustomerOrderRepository customerOrderRepository, ProductRepository productRepository,
			UserRepository userRepository) {
		this.customerOrderRepository = customerOrderRepository;
		this.productRepository = productRepository;
		this.userRepository = userRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public Page<CustomerOrder> getOrders(Long actorUserId, Role actorRole, Pageable pageable) {
		User customer = findActiveCustomer(actorUserId, actorRole);
		Page<CustomerOrder> page = customerOrderRepository.findByCustomerId(customer.getId(), pageable);
		if (page.isEmpty()) {
			return page;
		}
		List<Long> orderIds = page.getContent().stream().map(CustomerOrder::getId).toList();
		Map<Long, CustomerOrder> ordersById = customerOrderRepository
				.findByCustomerIdAndIdInWithItems(customer.getId(), orderIds)
				.stream()
				.collect(Collectors.toMap(CustomerOrder::getId, Function.identity()));
		List<CustomerOrder> orders = orderIds.stream().map(ordersById::get).toList();
		return new PageImpl<>(orders, pageable, page.getTotalElements());
	}

	@Override
	@Transactional(readOnly = true)
	public CustomerOrder getOrder(Long orderId, Long actorUserId, Role actorRole) {
		User customer = findActiveCustomer(actorUserId, actorRole);
		if (orderId == null || orderId <= 0) {
			throw new OrderNotFoundException();
		}
		return customerOrderRepository.findByIdAndCustomerId(orderId, customer.getId())
				.orElseThrow(OrderNotFoundException::new);
	}

	@Override
	@Transactional
	public CustomerOrder cancelOrder(Long orderId, Long actorUserId, Role actorRole) {
		User customer = findActiveCustomer(actorUserId, actorRole);
		if (orderId == null || orderId <= 0) {
			throw new OrderNotFoundException();
		}
		CustomerOrder order = customerOrderRepository.findByIdAndCustomerIdForUpdate(orderId, customer.getId())
				.orElseThrow(OrderNotFoundException::new);
		order.cancel();
		List<OrderItem> items = order.getItems().stream()
				.sorted(Comparator.comparing(item -> item.getProduct().getId()))
				.toList();
		List<ProductRestoration> products = items.stream().map(this::lockProductForRestoration).toList();
		for (ProductRestoration product : products) {
			product.product().restoreStock(product.quantity());
		}
		return customerOrderRepository.save(order);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<CustomerOrder> getVendorOrders(Long actorUserId, Role actorRole, Pageable pageable) {
		User vendor = findActiveVendor(actorUserId, actorRole);
		Page<CustomerOrder> page = customerOrderRepository.findDistinctByVendorId(vendor.getId(), pageable);
		if (page.isEmpty()) {
			return page;
		}
		List<Long> orderIds = page.getContent().stream().map(CustomerOrder::getId).toList();
		Map<Long, CustomerOrder> ordersById = customerOrderRepository
				.findByVendorIdAndIdInWithItems(vendor.getId(), orderIds).stream()
				.collect(Collectors.toMap(CustomerOrder::getId, Function.identity()));
		return new PageImpl<>(orderIds.stream().map(ordersById::get).toList(), pageable, page.getTotalElements());
	}

	@Override
	@Transactional(readOnly = true)
	public VendorOrderStatisticsResponse getVendorStatistics(Long actorUserId, Role actorRole) {
		User vendor = findActiveVendor(actorUserId, actorRole);
		return new VendorOrderStatisticsResponse(
				customerOrderRepository.countVendorOrders(vendor.getId()),
				customerOrderRepository.countVendorOrdersByStatus(vendor.getId(), OrderStatus.PLACED),
				customerOrderRepository.countVendorOrdersByStatus(vendor.getId(), OrderStatus.FULFILLED),
				customerOrderRepository.countVendorOrdersByStatus(vendor.getId(), OrderStatus.CANCELLED),
				customerOrderRepository.sumVendorUnits(vendor.getId()),
				nullSafe(customerOrderRepository.sumVendorSales(vendor.getId())));
	}

	@Override
	@Transactional(readOnly = true)
	public Page<CustomerOrder> getAdminOrders(Long actorUserId, Role actorRole, Pageable pageable) {
		findActiveAdmin(actorUserId, actorRole);
		Page<CustomerOrder> page = customerOrderRepository.findAll(pageable);
		if (page.isEmpty()) {
			return page;
		}
		List<Long> ids = page.getContent().stream().map(CustomerOrder::getId).toList();
		Map<Long, CustomerOrder> byId = customerOrderRepository.findAllWithItemsById(ids).stream()
				.collect(Collectors.toMap(CustomerOrder::getId, Function.identity()));
		return new PageImpl<>(ids.stream().map(byId::get).toList(), pageable, page.getTotalElements());
	}

	@Override
	@Transactional
	public CustomerOrder updateFulfillmentStatus(Long orderId, Long itemId, FulfillmentStatus status,
			Long actorUserId, Role actorRole) {
		User vendor = findActiveVendor(actorUserId, actorRole);
		CustomerOrder order = customerOrderRepository.findByIdWithItems(orderId)
				.orElseThrow(OrderNotFoundException::new);
		if (order.getStatus() == OrderStatus.CANCELLED) {
			throw new IllegalArgumentException("Cancelled orders cannot be updated");
		}
		OrderItem item = order.getItems().stream().filter(candidate -> candidate.getId().equals(itemId)).findFirst()
				.orElseThrow(OrderNotFoundException::new);
		if (item.getVendor() == null || !vendor.getId().equals(item.getVendor().getId())) {
			throw accessDenied();
		}
		boolean valid = status == FulfillmentStatus.SHIPPED && item.getFulfillmentStatus() == FulfillmentStatus.PLACED
				|| status == FulfillmentStatus.DELIVERED && item.getFulfillmentStatus() == FulfillmentStatus.SHIPPED;
		if (!valid) {
			throw new IllegalArgumentException("Invalid fulfillment transition");
		}
		item.setFulfillmentStatus(status);
		if (order.getItems().stream().allMatch(candidate -> candidate.getFulfillmentStatus() == FulfillmentStatus.DELIVERED)) {
			order.setStatus(OrderStatus.FULFILLED);
		}
		return customerOrderRepository.save(order);
	}

	@Override
	@Transactional(readOnly = true)
	public AdminOrderStatisticsResponse getAdminStatistics(Long actorUserId, Role actorRole) {
		findActiveAdmin(actorUserId, actorRole);
		return new AdminOrderStatisticsResponse(
				customerOrderRepository.countAllOrders(),
				customerOrderRepository.countAllOrdersByStatus(OrderStatus.PLACED),
				customerOrderRepository.countAllOrdersByStatus(OrderStatus.FULFILLED),
				customerOrderRepository.countAllOrdersByStatus(OrderStatus.CANCELLED),
				customerOrderRepository.sumAllUnits(),
				nullSafe(customerOrderRepository.sumAllSales()));
	}

	private ProductRestoration lockProductForRestoration(OrderItem item) {
		Integer quantity = item.getQuantity();
		if (quantity == null || quantity <= 0) {
			throw new IllegalStateException("Invalid order item quantity");
		}
		Long productId = item.getProduct().getId();
		Product product = productRepository.findByIdForUpdate(productId).orElseThrow(ProductNotFoundException::new);
		return new ProductRestoration(product, quantity);
	}

	private User findActiveCustomer(Long actorUserId, Role actorRole) {
		if (actorUserId == null || actorUserId <= 0 || actorRole != Role.CUSTOMER) {
			throw accessDenied();
		}
		User user = userRepository.findById(actorUserId).orElseThrow(this::accessDenied);
		if (user.getId() == null || !actorUserId.equals(user.getId()) || !user.isActive()
				|| user.getRole() != Role.CUSTOMER || user.getRole() != actorRole) {
			throw accessDenied();
		}
		return user;
	}

	private User findActiveVendor(Long actorUserId, Role actorRole) {
		if (actorUserId == null || actorUserId <= 0 || actorRole != Role.VENDOR) {
			throw accessDenied();
		}
		User user = userRepository.findById(actorUserId).orElseThrow(this::accessDenied);
		if (!actorUserId.equals(user.getId()) || !user.isActive()
				|| user.getRole() != Role.VENDOR || user.getRole() != actorRole) {
			throw accessDenied();
		}
		return user;
	}

	private User findActiveAdmin(Long actorUserId, Role actorRole) {
		if (actorUserId == null || actorUserId <= 0 || actorRole != Role.ADMIN) {
			throw accessDenied();
		}
		User user = userRepository.findById(actorUserId).orElseThrow(this::accessDenied);
		if (!actorUserId.equals(user.getId()) || !user.isActive()
				|| user.getRole() != Role.ADMIN || user.getRole() != actorRole) {
			throw accessDenied();
		}
		return user;
	}

	private BigDecimal nullSafe(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	private AccessDeniedException accessDenied() {
		return new AccessDeniedException("Access denied");
	}

	private record ProductRestoration(Product product, Integer quantity) {
	}
}
