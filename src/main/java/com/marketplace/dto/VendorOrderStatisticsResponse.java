package com.marketplace.dto;

import java.math.BigDecimal;

public record VendorOrderStatisticsResponse(
		long totalOrders,
		long placedOrders,
		long fulfilledOrders,
		long cancelledOrders,
		long totalUnits,
		BigDecimal totalSales
) {
}
