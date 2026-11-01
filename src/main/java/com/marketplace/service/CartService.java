package com.marketplace.service;

import com.marketplace.entity.Cart;
import com.marketplace.entity.CartItem;
import com.marketplace.enums.Role;

public interface CartService {

	Cart getCart(Long actorUserId, Role actorRole);

	Cart addItem(Long productId, Integer quantity, Long actorUserId, Role actorRole);

	CartItem updateItem(Long itemId, Integer quantity, Long actorUserId, Role actorRole);

	void removeItem(Long itemId, Long actorUserId, Role actorRole);
}
