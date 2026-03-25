package com.teleticket.web.dto.Response;

public record OrderItemResponse(
        Long id,
        Long order,
        Long seatId,
        Long performanceId,
        Double unitPrice

) {
}
