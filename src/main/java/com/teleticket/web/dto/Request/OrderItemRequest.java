package com.teleticket.web.dto.Request;

public record OrderItemRequest(
        Long order,
        Long seatId,
        Long performanceId,
        Double unitPrice

) {
}
