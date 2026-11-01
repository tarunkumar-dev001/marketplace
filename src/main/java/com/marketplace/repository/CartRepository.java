package com.marketplace.repository;

import com.marketplace.entity.Cart;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

	@EntityGraph(attributePaths = {"items", "items.product"})
	Optional<Cart> findByCustomerId(Long customerId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = {"items", "items.product"})
	@Query("select c from Cart c where c.customer.id = :customerId")
	Optional<Cart> findByCustomerIdForUpdate(@Param("customerId") Long customerId);
}
