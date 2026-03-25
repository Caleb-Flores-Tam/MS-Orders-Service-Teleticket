package com.teleticket.web.dto.Request;

import lombok.Builder;

import java.util.List;

@Builder
public record OrdersRequest(
        Long userId,            // ID del usuario que compra
        Long performanceId,     // Necesario para inventoryClient.getAvailability()
        List<Long> seatIds,     // Lista de IDs de asientos a comprar
        Double totalAmount,     // Monto total
        Double taxAmount        // Impuestos
) {
}
