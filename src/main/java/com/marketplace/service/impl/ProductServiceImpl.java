package com.marketplace.service.impl;

import com.marketplace.entity.Product;
import com.marketplace.entity.User;
import com.marketplace.enums.Role;
import com.marketplace.exception.ProductNotFoundException;
import com.marketplace.repository.ProductRepository;
import com.marketplace.repository.UserRepository;
import com.marketplace.service.ProductService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

	private final ProductRepository productRepository;
	private final UserRepository userRepository;

	public ProductServiceImpl(ProductRepository productRepository, UserRepository userRepository) {
		this.productRepository = productRepository;
		this.userRepository = userRepository;
	}

	@Override
	@Transactional
	public Product create(Product product, Long actorUserId, Role actorRole) {
		User actor = findActiveActor(actorUserId, actorRole);
		if (actor.getRole() == Role.ADMIN) {
			product.assignVendor(null);
			return productRepository.save(product);
		}
		if (actor.getRole() == Role.VENDOR) {
			product.assignVendor(actor);
			return productRepository.save(product);
		}
		throw accessDenied();
	}

	@Override
	public Product getById(Long id) {
		return findProductById(id);
	}

	@Override
	public List<Product> getAll() {
		return productRepository.findAll();
	}

	@Override
	@Transactional
	public Product update(Long id, Product product, Long actorUserId, Role actorRole) {
		User actor = findActiveActor(actorUserId, actorRole);
		Product existingProduct = findProductById(id);
		authorizeProductManagement(existingProduct, actor);
		existingProduct.update(
				product.getName(),
				product.getDescription(),
				product.getPrice(),
				product.getStockQuantity(),
				product.getCategory(),
				product.isActive()
		);
		return productRepository.save(existingProduct);
	}

	@Override
	@Transactional
	public void delete(Long id, Long actorUserId, Role actorRole) {
		User actor = findActiveActor(actorUserId, actorRole);
		Product product = findProductById(id);
		authorizeProductManagement(product, actor);
		productRepository.delete(product);
	}

	private Product findProductById(Long id) {
		return productRepository.findById(id)
				.orElseThrow(ProductNotFoundException::new);
	}

	private void authorizeProductManagement(Product product, User actor) {
		if (actor.getRole() == Role.ADMIN) {
			return;
		}
		User vendor = product.getVendor();
		if (vendor == null || !actor.getId().equals(vendor.getId())) {
			throw accessDenied();
		}
	}

	private User findActiveActor(Long actorUserId, Role actorRole) {
		if (actorUserId == null || actorUserId <= 0 || actorRole == null) {
			throw accessDenied();
		}
		User user = userRepository.findById(actorUserId).orElseThrow(this::accessDenied);
		if (user.getId() == null || !actorUserId.equals(user.getId()) || !user.isActive()
				|| user.getRole() != actorRole || (actorRole != Role.VENDOR && actorRole != Role.ADMIN)) {
			throw accessDenied();
		}
		return user;
	}

	private AccessDeniedException accessDenied() {
		return new AccessDeniedException("Access denied");
	}
}
