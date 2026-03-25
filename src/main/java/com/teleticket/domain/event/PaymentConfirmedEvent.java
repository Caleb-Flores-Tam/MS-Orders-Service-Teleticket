package com.teleticket.domain.event;

import java.util.List;

/**
 * Published to the orders.exchange with routing key "payment.confirmed"
 * when a Yape/MercadoPago payment is approved.
 *
 * Fields must match what inventory-service and ticketing-service expect:
 *   orderId       – String  (order DB id as string)
 *   performanceId – Long    (used by inventory to look up availability records)
 *   seatIds       – List<Long>
 *   userId        – Long
 *   totalAmount   – Double  (informational, used by ticketing for the email)
 *   userEmail     – String  (used by ticketing to send the confirmation email)
 */
public record PaymentConfirmedEvent(
        String orderId,
        Long performanceId,
        List<Long> seatIds,
        Long userId,
        Double totalAmount,
        String userEmail
) {}

