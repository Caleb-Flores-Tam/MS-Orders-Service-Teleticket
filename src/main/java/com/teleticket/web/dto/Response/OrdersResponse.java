package com.teleticket.web.dto.Response;

import lombok.Builder;

import java.util.List;

@Builder
public record OrdersResponse(
        Long id,
        Long user_id,
        String email,
        Double totalAmount,
        Double taxAmount,
        String status,
        List<OrderItemResponse> items // Añade esta línea
) { }
