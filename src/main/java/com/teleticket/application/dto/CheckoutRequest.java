package com.teleticket.application.dto;

import com.teleticket.web.dto.Request.OrdersRequest;

public record CheckoutRequest(
        Long orderId,           // ID de la orden que se va a pagar
        String paymentToken,    // Token generado en el front
        String customerEmail    // Email para Mercado Pago
) {}
