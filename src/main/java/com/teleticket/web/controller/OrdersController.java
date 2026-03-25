package com.teleticket.web.controller;

import com.teleticket.application.dto.CheckoutRequest;
import com.teleticket.application.dto.YapePaymentRequest;
import com.teleticket.application.dto.YapePaymentResponse;
import com.teleticket.application.service.OrderService;
import com.teleticket.web.dto.Request.OrdersRequest;
import com.teleticket.web.dto.Response.OrdersResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrdersController {

    private final OrderService orderService;

    /**
     * 1. Crea una orden inicial validando inventario y precios.
     * Estado inicial: PENDING
     */
    @PostMapping
    public ResponseEntity<OrdersResponse> createOrder(@RequestBody OrdersRequest request) {
        OrdersResponse response = orderService.createOrder(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * 2. Procesa el pago con Yape (Mercado Pago).
     * Actualiza el estado de la orden a PAID si el pago es exitoso.
     */
    @PostMapping("/checkout")
    public ResponseEntity<OrdersResponse> processPayment(@RequestBody CheckoutRequest request) {
        OrdersResponse response = orderService.processYapePayment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 3. Procesa un pago directo con Yape (Mercado Pago) sin necesidad de una orden previa.
     * Recibe token, payerEmail, amount y description directamente del frontend.
     * Retorna el resultado del pago (approved/rejected).
     */
    @PostMapping("/pay/{orderId}")
    public ResponseEntity<YapePaymentResponse> processDirectPayment(
            @PathVariable Long orderId,
            @RequestBody YapePaymentRequest request) {
        YapePaymentResponse response = orderService.processDirectYapePayment(request, orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * 4. Consulta el detalle y estado de una orden por su ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrdersResponse> getOrderById(@PathVariable Long id) {
        OrdersResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 5. (Opcional) Listar todas las órdenes de un usuario específico.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrdersResponse>> getOrdersByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }

    @PatchMapping("/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrdersResponse> refundOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.refundOrder(id));
    }

}
