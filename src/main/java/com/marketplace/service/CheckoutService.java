package com.marketplace.service;

import com.marketplace.entity.CustomerOrder;
import com.marketplace.enums.Role;
import com.marketplace.dto.CheckoutRequest;

public interface CheckoutService {

	CustomerOrder checkout(Long actorUserId, Role actorRole);
	CustomerOrder checkout(Long actorUserId, Role actorRole, CheckoutRequest request);
}
