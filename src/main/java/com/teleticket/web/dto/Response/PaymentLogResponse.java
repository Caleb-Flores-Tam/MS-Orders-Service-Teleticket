package com.teleticket.web.dto.Response;

public record PaymentLogResponse(
        Long id,
        Long order,
        String transactionRef,
        String paymentMethod,
        String gatewayResponse
) {
}
