package com.teleticket.web.dto.Request;

public record PaymentLogRequest(
        Long order,
        String transactionRef,
        String paymentMethod,
        String gatewayResponse

) {
}
