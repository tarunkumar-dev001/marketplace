package com.marketplace.dto;

import com.marketplace.enums.FulfillmentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateFulfillmentStatusRequest(@NotNull FulfillmentStatus status) {
}
