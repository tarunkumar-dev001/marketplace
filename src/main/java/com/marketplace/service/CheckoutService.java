package com.marketplace.service;

import com.marketplace.entity.CustomerOrder;
import com.marketplace.enums.Role;

public interface CheckoutService {

	CustomerOrder checkout(Long actorUserId, Role actorRole);
}
