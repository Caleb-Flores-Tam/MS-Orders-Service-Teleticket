package com.teleticket.config.security;

/**
 * RabbitMQ constants shared across the order-service.
 * The exchange and routing keys must match what inventory-service and ticketing-service declare.
 */
public class RabbitMQConfig {

    // ─── Exchange (same for all order events) ─────────────────────────────────
    public static final String ORDERS_EXCHANGE = "orders.exchange";

    // ─── Routing keys ─────────────────────────────────────────────────────────
    public static final String ROUTING_KEY_PAYMENT_CONFIRMED = "payment.confirmed";
    public static final String ROUTING_KEY_ORDER_EXPIRED     = "order.expired";
    public static final String ROUTING_KEY_ORDER_CANCELLED   = "order.cancelled";
}
