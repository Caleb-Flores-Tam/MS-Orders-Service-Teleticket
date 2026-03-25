package com.teleticket.application.service;

import com.teleticket.application.dto.CheckoutRequest;
import com.teleticket.application.dto.YapePaymentRequest;
import com.teleticket.application.dto.YapePaymentResponse;
import com.teleticket.domain.entity.OrderStatus;
import com.teleticket.domain.entity.Orders;
import com.teleticket.web.dto.Request.OrdersRequest;
import com.teleticket.web.dto.Response.OrdersResponse;

import java.util.List;
import java.util.Optional;

public interface OrderService {
    public OrdersResponse createOrder(OrdersRequest request);
    public OrdersResponse processYapePayment(CheckoutRequest request);
    public YapePaymentResponse processDirectYapePayment(YapePaymentRequest request, Long orderId);
    public OrdersResponse getOrderById(Long id);
    List<OrdersResponse> getOrdersByUserId(Long userId);
    OrdersResponse refundOrder(Long id);
}
