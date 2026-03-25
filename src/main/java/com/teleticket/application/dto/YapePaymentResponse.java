package com.teleticket.application.dto;

public record YapePaymentResponse(
        Long paymentId,       // ID del pago devuelto por Mercado Pago
        String status,        // "approved" | "rejected" | etc.
        String statusDetail,  // Detalle adicional del estado
        Double amount,        // Monto cobrado
        String description    // Descripción del pago
) {}
