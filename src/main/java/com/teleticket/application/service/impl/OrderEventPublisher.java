package com.teleticket.application.service.impl;

import com.teleticket.config.security.RabbitMQConfig;
import com.teleticket.domain.event.OrderCancelledEvent;
import com.teleticket.domain.event.PaymentConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishOrderConfirmed(PaymentConfirmedEvent event) {
        log.info("Publishing payment.confirmed for orderId={}", event.orderId());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDERS_EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_PAYMENT_CONFIRMED,
                event);
    }

    public void publishOrderExpired(Long orderId, Long performanceId, java.util.List<Long> seatIds, Long userId) {
        OrderCancelledEvent event = new OrderCancelledEvent(
                orderId.toString(), performanceId, seatIds, userId);
        log.info("Publishing order.expired for orderId={}", orderId);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDERS_EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_ORDER_EXPIRED,
                event);
    }

    public void publishOrderCancelled(Long orderId, Long performanceId, java.util.List<Long> seatIds, Long userId) {
        OrderCancelledEvent event = new OrderCancelledEvent(
                orderId.toString(), performanceId, seatIds, userId);
        log.info("Publishing order.cancelled for orderId={}", orderId);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDERS_EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_ORDER_CANCELLED,
                event);
    }
}

