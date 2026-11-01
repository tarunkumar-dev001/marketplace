package com.marketplace.repository;

import com.marketplace.entity.CartItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
	boolean existsByProductId(Long productId);

	@EntityGraph(attributePaths = "product")
	Optional<CartItem> findByIdAndCartCustomerId(Long id, Long customerId);
}
