package com.teleticket.application.mapper;

import com.teleticket.application.client.UserResponse;
import com.teleticket.domain.entity.OrderItem;
import com.teleticket.domain.entity.Orders;
import com.teleticket.web.dto.Request.OrdersRequest;
import com.teleticket.web.dto.Response.OrderItemResponse;
import com.teleticket.web.dto.Response.OrdersResponse;

import java.util.List;

public interface OrderMapper {
    Orders toEntity (OrdersRequest request);
    OrdersResponse toDto (Orders orders, UserResponse userResponse);
    List<OrdersResponse> toDtoList(List<Orders> orders);
    OrderItemResponse toItemDto(OrderItem item);
    }
