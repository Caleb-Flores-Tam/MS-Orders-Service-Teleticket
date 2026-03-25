package com.teleticket.domain.event;

import java.util.List;

/**
 * Published to orders.exchange with routing key "order.expired" or "order.cancelled"
 * so that inventory-service can transition seats from HELD → AVAILABLE.
 *
 * Must match the structure inventory-service's OrderCancelledEvent expects.
 */
public record OrderCancelledEvent(
        String orderId,
        Long performanceId,
        List<Long> seatIds,
        Long userId
) {}
