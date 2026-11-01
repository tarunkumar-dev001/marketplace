package com.marketplace.service.impl;

import com.marketplace.entity.Cart;
import com.marketplace.entity.CartItem;
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
import com.marketplace.dto.CheckoutRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
public class CheckoutServiceImpl implements CheckoutService {

	private static final String ORDER_CURRENCY = "INR";

	private final CartRepository cartRepository;
	private final CustomerOrderRepository customerOrderRepository;
	private final ProductRepository productRepository;
	private final UserRepository userRepository;

	public CheckoutServiceImpl(CartRepository cartRepository, CustomerOrderRepository customerOrderRepository,
			ProductRepository productRepository, UserRepository userRepository) {
		this.cartRepository = cartRepository;
		this.customerOrderRepository = customerOrderRepository;
		this.productRepository = productRepository;
		this.userRepository = userRepository;
	}

	@Override
	@Transactional
	public CustomerOrder checkout(Long actorUserId, Role actorRole) {
		return checkout(actorUserId, actorRole, new CheckoutRequest("", "", "", "", "", com.marketplace.enums.PaymentMethod.COD));
	}

	@Override
	@Transactional
	public CustomerOrder checkout(Long actorUserId, Role actorRole, CheckoutRequest request) {
		User customer = findActiveCustomer(actorUserId, actorRole);
		Cart cart = cartRepository.findByCustomerIdForUpdate(customer.getId()).orElseThrow(CartEmptyException::new);
		List<CartItem> cartItems = cart.getItems().stream()
				.sorted(Comparator.comparing(item -> item.getProduct().getId()))
				.toList();
		if (cartItems.isEmpty()) {
			throw new CartEmptyException();
		}

		List<CheckoutLine> lines = cartItems.stream()
				.map(this::lockAndValidateProduct)
				.toList();
		BigDecimal totalAmount = lines.stream().map(CheckoutLine::lineTotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		CustomerOrder order = new CustomerOrder(customer, OrderStatus.PLACED, ORDER_CURRENCY, totalAmount);
		order.setShippingAndPayment(request.name(), request.phone(), request.address(), request.city(), request.pincode(), request.paymentMethod());
		for (CheckoutLine line : lines) {
			line.product().decrementStock(line.quantity());
			order.addItem(line.product(), line.product().getVendor(), line.product().getName(), line.product().getPrice(),
					line.quantity(), line.lineTotal());
		}
		CustomerOrder savedOrder = customerOrderRepository.save(order);
		cart.clearItems();
		return savedOrder;
	}

	private CheckoutLine lockAndValidateProduct(CartItem cartItem) {
		Integer quantity = cartItem.getQuantity();
		if (quantity == null || quantity <= 0) {
			throw new InsufficientStockException();
		}
		Long productId = cartItem.getProduct().getId();
		Product product = productRepository.findByIdAndActiveTrueForUpdate(productId)
				.orElseThrow(ProductNotFoundException::new);
		if (product.getStockQuantity() == null || product.getStockQuantity() < quantity) {
			throw new InsufficientStockException();
		}
		return new CheckoutLine(product, quantity, product.getPrice().multiply(BigDecimal.valueOf(quantity)));
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

	private AccessDeniedException accessDenied() {
		return new AccessDeniedException("Access denied");
	}

	private record CheckoutLine(Product product, Integer quantity, BigDecimal lineTotal) {
	}
}
