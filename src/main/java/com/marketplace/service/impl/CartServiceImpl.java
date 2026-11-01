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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartServiceImpl implements CartService {

	private final CartRepository cartRepository;
	private final CartItemRepository cartItemRepository;
	private final ProductRepository productRepository;
	private final UserRepository userRepository;

	public CartServiceImpl(CartRepository cartRepository, CartItemRepository cartItemRepository,
			ProductRepository productRepository, UserRepository userRepository) {
		this.cartRepository = cartRepository;
		this.cartItemRepository = cartItemRepository;
		this.productRepository = productRepository;
		this.userRepository = userRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public Cart getCart(Long actorUserId, Role actorRole) {
		User customer = findActiveCustomer(actorUserId, actorRole);
		return cartRepository.findByCustomerId(customer.getId()).orElseGet(() -> new Cart(customer));
	}

	@Override
	@Transactional
	public Cart addItem(Long productId, Integer quantity, Long actorUserId, Role actorRole) {
		requirePositiveQuantity(quantity);
		User customer = findActiveCustomer(actorUserId, actorRole);
		Product product = findActiveProduct(productId);
		Cart cart = cartRepository.findByCustomerId(customer.getId()).orElseGet(() -> new Cart(customer));

		CartItem existingItem = cart.getItems().stream()
				.filter(item -> product.getId().equals(item.getProduct().getId()))
				.findFirst()
				.orElse(null);
		if (existingItem == null) {
			cart.addItem(product, quantity);
		} else {
			existingItem.updateQuantity(addQuantities(existingItem.getQuantity(), quantity));
		}
		return cartRepository.save(cart);
	}

	@Override
	@Transactional
	public CartItem updateItem(Long itemId, Integer quantity, Long actorUserId, Role actorRole) {
		requirePositiveQuantity(quantity);
		User customer = findActiveCustomer(actorUserId, actorRole);
		CartItem item = findCustomerCartItem(itemId, customer.getId());
		findActiveProduct(item.getProduct().getId());
		item.updateQuantity(quantity);
		return cartItemRepository.save(item);
	}

	@Override
	@Transactional
	public void removeItem(Long itemId, Long actorUserId, Role actorRole) {
		User customer = findActiveCustomer(actorUserId, actorRole);
		cartItemRepository.delete(findCustomerCartItem(itemId, customer.getId()));
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

	private Product findActiveProduct(Long productId) {
		if (productId == null || productId <= 0) {
			throw new ProductNotFoundException();
		}
		return productRepository.findByIdAndActiveTrue(productId).orElseThrow(ProductNotFoundException::new);
	}

	private CartItem findCustomerCartItem(Long itemId, Long customerId) {
		if (itemId == null || itemId <= 0) {
			throw new CartItemNotFoundException();
		}
		return cartItemRepository.findByIdAndCartCustomerId(itemId, customerId)
				.orElseThrow(CartItemNotFoundException::new);
	}

	private void requirePositiveQuantity(Integer quantity) {
		if (quantity == null || quantity <= 0) {
			throw new IllegalArgumentException("Quantity must be positive");
		}
	}

	private int addQuantities(Integer existingQuantity, Integer quantity) {
		try {
			return Math.addExact(existingQuantity, quantity);
		} catch (ArithmeticException exception) {
			throw new IllegalArgumentException("Quantity exceeds supported range");
		}
	}

	private AccessDeniedException accessDenied() {
		return new AccessDeniedException("Access denied");
	}
}
