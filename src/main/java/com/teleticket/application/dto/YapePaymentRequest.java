package com.teleticket.application.dto;

public record YapePaymentRequest(
        String token,         // Token generado por el SDK de Yape en el frontend
        String payerEmail,    // Correo del comprador
        Double amount,        // Monto a cobrar
        String description    // Descripción del pago
) {}
