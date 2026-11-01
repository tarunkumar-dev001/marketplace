package com.marketplace.service;

import com.marketplace.entity.CustomerOrder;
import com.marketplace.enums.Role;
import com.marketplace.dto.AdminOrderStatisticsResponse;
import com.marketplace.dto.VendorOrderStatisticsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

	Page<CustomerOrder> getOrders(Long actorUserId, Role actorRole, Pageable pageable);

	CustomerOrder getOrder(Long orderId, Long actorUserId, Role actorRole);

	CustomerOrder cancelOrder(Long orderId, Long actorUserId, Role actorRole);

	Page<CustomerOrder> getVendorOrders(Long actorUserId, Role actorRole, Pageable pageable);

	VendorOrderStatisticsResponse getVendorStatistics(Long actorUserId, Role actorRole);

	Page<CustomerOrder> getAdminOrders(Long actorUserId, Role actorRole, Pageable pageable);

	AdminOrderStatisticsResponse getAdminStatistics(Long actorUserId, Role actorRole);
}
