package com.marketplace.service;

import com.marketplace.entity.Product;
import com.marketplace.enums.Role;

import java.util.List;

public interface ProductService {

	Product create(Product product, Long actorUserId, Role actorRole);

	Product getById(Long id);

	List<Product> getAll();

	Product update(Long id, Product product, Long actorUserId, Role actorRole);

	void delete(Long id, Long actorUserId, Role actorRole);
}
