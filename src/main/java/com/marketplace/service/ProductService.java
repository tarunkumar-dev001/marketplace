package com.marketplace.service;

import com.marketplace.entity.Product;
import com.marketplace.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface ProductService {

	Product create(Product product, Long actorUserId, Role actorRole);

	Product getById(Long id);

	Page<Product> getAll(Pageable pageable, String category, String search);

	Page<Product> getMine(Pageable pageable, Long actorUserId, Role actorRole);

	Page<Product> getAllForAdmin(Pageable pageable, Long actorUserId, Role actorRole);

	Product updateStatus(Long id, boolean active, Long actorUserId, Role actorRole);

	Product update(Long id, Product product, Long actorUserId, Role actorRole);

	void delete(Long id, Long actorUserId, Role actorRole);
}
