package com.teleticket.application.mapper.impl;

import com.teleticket.application.client.UserResponse;
import com.teleticket.application.mapper.OrderMapper;
import com.teleticket.domain.entity.OrderItem;
import com.teleticket.domain.entity.OrderStatus;
import com.teleticket.domain.entity.Orders;
import com.teleticket.web.dto.Request.OrdersRequest;
import com.teleticket.web.dto.Response.OrderItemResponse;
import com.teleticket.web.dto.Response.OrdersResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapperImpl implements OrderMapper {
    @Override
    public Orders toEntity(OrdersRequest request) {
        if (request == null) return null;

        return Orders.builder()
                // El userId lo extraemos del token en el Service,
                // pero lo mapeamos aquí si viene en el request.
                .userId(request.userId())
                .totalAmount(request.totalAmount())
                .taxAmount(request.taxAmount())
                .build();
    }

    @Override
    public OrdersResponse toDto(Orders orders, UserResponse userResponse) {
        if (orders == null) return null;

        return OrdersResponse.builder()
                .id(orders.getId())
                .user_id(orders.getUserId())
                .email(userResponse != null ? userResponse.email() : "Guest")
                .totalAmount(orders.getTotalAmount())
                .taxAmount(orders.getTaxAmount())
                .status(orders.getStatus() != null ? orders.getStatus().name() : OrderStatus.PENDING.name())
                .items(orders.getItems() != null ?
                        orders.getItems().stream().map(this::toItemDto).toList() : // .toList() es más directo
                        List.<OrderItemResponse>of())
                .build();
    }

    @Override
    public List<OrdersResponse> toDtoList(List<Orders> orders) {
        if (orders == null) return List.of();

        // Implementación real para que el listado del frontend funcione
        return orders.stream()
                .map(order -> toDto(order, null)) // Mapeamos cada orden
                .toList();
    }

    @Override
    public OrderItemResponse toItemDto(OrderItem item) {
        if (item == null) return null;

        return new OrderItemResponse(
                item.getId(),
                item.getOrder().getId(),
                item.getSeatId(),
                item.getPerformanceId(),
                item.getUnitPrice()
        );
    }
}
