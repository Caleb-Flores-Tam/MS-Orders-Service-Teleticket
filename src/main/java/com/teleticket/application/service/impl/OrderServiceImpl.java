package com.teleticket.application.service.impl;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.teleticket.application.client.InventoryClient;
import com.teleticket.application.dto.AvailabilityResponseDTO;
import com.teleticket.application.dto.CheckoutRequest;
import com.teleticket.application.dto.SeatAvailabilityDTO;
import com.teleticket.application.dto.YapePaymentRequest;
import com.teleticket.application.dto.YapePaymentResponse;
import com.teleticket.application.mapper.OrderMapper;
import com.teleticket.application.service.OrderService;
import com.teleticket.domain.entity.OrderItem;
import com.teleticket.domain.entity.OrderStatus;
import com.teleticket.domain.entity.Orders;
import com.teleticket.domain.entity.PaymentLog;
import com.teleticket.domain.event.PaymentConfirmedEvent;
import com.teleticket.domain.repository.OrderItemRepository;
import com.teleticket.domain.repository.OrdersRepository;
import com.teleticket.domain.repository.PaymentLogRepository;
import com.teleticket.web.dto.Request.OrdersRequest;
import com.teleticket.web.dto.Response.OrdersResponse;
import com.teleticket.web.exception.BusinessException;
import com.teleticket.web.exception.InventoryException;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    @Value("${mercadopago.access-token}")
    private String mpAccessToken;
    //Rabbit
    private final OrderEventPublisher eventPublisher;

    private final OrdersRepository ordersRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final InventoryClient inventoryClient;
    private final OrderMapper orderMapper;

    @PostConstruct
    public void initMercadoPago() {
        if (mpAccessToken != null && !mpAccessToken.isBlank()) {
            MercadoPagoConfig.setAccessToken(mpAccessToken);
            System.out.println("AccessToken configurado correctamente");
        } else {
            System.out.println("AccessToken es NULL o vacío");
        }
    }

    @Override
    @Transactional
    public OrdersResponse createOrder(OrdersRequest request) {
        // 1. Extraer uid del claim "uid" del JWT (no del claim "sub" que contiene el email)
        Long currentUserId = extractUidFromJwt();

        // 2. Obtener disponibilidad y validar asientos solicitados
        AvailabilityResponseDTO availability = inventoryClient.getAvailability(request.performanceId());

        List<SeatAvailabilityDTO> selectedSeats = availability.getSeats().stream()
                .filter(seat -> request.seatIds().contains(seat.getSeatId()))
                .toList();

        // Validar que todos los asientos solicitados existan en el resultado
        if (selectedSeats.size() != request.seatIds().size()) {
            throw new InventoryException("Uno o más asientos no existen para esta función.");
        }

        // Aceptar asientos AVAILABLE o HELD por el mismo usuario (el frontend reserva antes de crear la orden)
        for (SeatAvailabilityDTO seat : selectedSeats) {
            boolean isAvailable = "AVAILABLE".equals(seat.getStatus());
            boolean isHeldByMe = "HELD".equals(seat.getStatus())
                    && currentUserId.equals(seat.getLockedByUserId());
            if (!isAvailable && !isHeldByMe) {
                throw new InventoryException("El asiento " + seat.getSeatId() + " no está disponible (estado: " + seat.getStatus() + ").");
            }
        }

        // 3. Crear y guardar la Orden inicial (PENDING) vinculada al usuario del token
        Orders order = orderMapper.toEntity(request);
        order.setUserId(currentUserId);
        order.setStatus(OrderStatus.PENDING);
        Orders savedOrder = ordersRepository.save(order);

        // 4. Calcular precios y crear OrderItems
        double subtotal = 0.0;

        for (SeatAvailabilityDTO seat : selectedSeats) {
            Double unitPrice = inventoryClient.getPrice(request.performanceId(), seat.getZoneId());

            if (unitPrice == null || unitPrice <= 0) {
                throw new InventoryException("No se pudo obtener el precio para la zona: " + seat.getZoneId());
            }

            OrderItem item = new OrderItem();
            item.setOrder(savedOrder);
            item.setSeatId(seat.getSeatId());
            item.setPerformanceId(request.performanceId());
            item.setUnitPrice(unitPrice);

            orderItemRepository.save(item);
            subtotal += unitPrice;
        }

        // 5. Actualizar la orden con montos finales (Monto + 18% IGV)
        savedOrder.setTaxAmount(subtotal * 0.18);
        savedOrder.setTotalAmount(subtotal * 1.18);

        return orderMapper.toDto(ordersRepository.save(savedOrder), null);
    }

    @Override
    @Transactional
    public OrdersResponse processYapePayment(CheckoutRequest request) {
        // 1. Verificación de Seguridad: ¿La orden le pertenece al usuario del token?
        Long currentUserId = extractUidFromJwt();
        Orders order = ordersRepository.findByIdWithItems(request.orderId())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Orden no existe", HttpStatus.NOT_FOUND));

        if (!order.getUserId().equals(currentUserId)) {
            throw new BusinessException("FORBIDDEN", "No tienes permiso para pagar esta orden", HttpStatus.FORBIDDEN);
        }

        List<Long> seatIds = order.getItems() != null
                ? order.getItems().stream().map(OrderItem::getSeatId).toList()
                : List.of();

        Long performanceId = order.getItems() != null && !order.getItems().isEmpty()
                ? order.getItems().get(0).getPerformanceId()
                : null;

        try {
            // 2. Ejecutar cobro en Mercado Pago
            PaymentClient client = new PaymentClient();
            PaymentCreateRequest paymentRequest = PaymentCreateRequest.builder()
                    .transactionAmount(BigDecimal.valueOf(order.getTotalAmount()))
                    .token(request.paymentToken())
                    .description("Teleticket - Orden #" + order.getId())
                    .paymentMethodId("yape")
                    .payer(PaymentPayerRequest.builder().email(request.customerEmail()).build())
                    .build();

            Payment payment = client.create(paymentRequest);

            // 3. Registrar Auditoría (PaymentLog)
            PaymentLog log = PaymentLog.builder()
                    .order(order)
                    .transactionId(payment.getId() != null ? payment.getId().toString() : "N/A")
                    .paymentMethod("yape")
                    .amount(order.getTotalAmount())
                    .status(payment.getStatus())
                    .createdAt(LocalDateTime.now())
                    .build();
            paymentLogRepository.save(log);

            // 4. Actualizar estado final
            if ("approved".equals(payment.getStatus())) {
                order.setStatus(OrderStatus.PAID);
                ordersRepository.save(order);

                String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
                PaymentConfirmedEvent event = new PaymentConfirmedEvent(
                        order.getId().toString(),
                        performanceId,
                        seatIds,
                        order.getUserId(),
                        order.getTotalAmount(),
                        userEmail
                );
                eventPublisher.publishOrderConfirmed(event);

            } else {
                order.setStatus(OrderStatus.CANCELLED);
                ordersRepository.save(order);
                eventPublisher.publishOrderCancelled(order.getId(), performanceId, seatIds, order.getUserId());
                throw new BusinessException("PAYMENT_FAILED", "Pago rechazado: " + payment.getStatusDetail(), HttpStatus.BAD_REQUEST);
            }

            return orderMapper.toDto(ordersRepository.save(order), null);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            order.setStatus(OrderStatus.CANCELLED);
            ordersRepository.save(order);
            eventPublisher.publishOrderCancelled(order.getId(), performanceId, seatIds, order.getUserId());
            throw new BusinessException("MP_ERROR", "Error en pasarela: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public YapePaymentResponse processDirectYapePayment(YapePaymentRequest request, Long orderId) {
        // 0. Buscar la orden con sus items en una sola query (evita LazyInitializationException)
        Orders order = ordersRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "La orden no existe", HttpStatus.NOT_FOUND));

        // Extraer performanceId y seatIds desde los OrderItems de la orden
        List<Long> seatIds = order.getItems() != null
                ? order.getItems().stream().map(OrderItem::getSeatId).toList()
                : List.of();

        Long performanceId = order.getItems() != null && !order.getItems().isEmpty()
                ? order.getItems().get(0).getPerformanceId()
                : null;

        Long userId = order.getUserId();

        try {
            // 1. Validar tiempo límite de 10 minutos
            if (order.getCreatedAt().plusMinutes(10).isBefore(LocalDateTime.now())) {
                order.setStatus(OrderStatus.CANCELLED);
                ordersRepository.save(order);
                // Enviar evento de expiración con toda la información para liberar los asientos
                eventPublisher.publishOrderExpired(order.getId(), performanceId, seatIds, userId);
                throw new BusinessException("TIMEOUT", "El tiempo de pago (10 min) ha expirado", HttpStatus.GONE);
            }

            // 2. Construir la solicitud de pago
            PaymentClient client = new PaymentClient();
            PaymentCreateRequest paymentRequest = PaymentCreateRequest.builder()
                    .transactionAmount(BigDecimal.valueOf(request.amount()))
                    .token(request.token())
                    .description(request.description())
                    .paymentMethodId("yape")
                    .installments(1)
                    .payer(PaymentPayerRequest.builder().email(request.payerEmail()).build())
                    .build();

            // 3. Ejecutar cobro
            Payment payment = client.create(paymentRequest);

            // 4. Registrar auditoría vinculada a la orden
            PaymentLog log = PaymentLog.builder()
                    .order(order)
                    .transactionId(payment.getId() != null ? payment.getId().toString() : "N/A")
                    .paymentMethod("yape")
                    .amount(request.amount())
                    .status(payment.getStatus())
                    .createdAt(LocalDateTime.now())
                    .build();
            paymentLogRepository.save(log);

            // 5. Manejo de estados y envío de eventos RabbitMQ
            if ("approved".equals(payment.getStatus())) {
                order.setStatus(OrderStatus.PAID);
                ordersRepository.save(order);

                // Publicar PaymentConfirmedEvent al exchange orders.exchange con routing key payment.confirmed
                // Incluye performanceId (para inventory) y userEmail (para ticketing)
                String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
                PaymentConfirmedEvent event = new PaymentConfirmedEvent(
                        order.getId().toString(),
                        performanceId,
                        seatIds,
                        userId,
                        order.getTotalAmount(),
                        userEmail
                );
                eventPublisher.publishOrderConfirmed(event);

            } else {
                // ENVIAR EVENTO: order.cancelled (si MP rechaza)
                order.setStatus(OrderStatus.CANCELLED);
                ordersRepository.save(order);
                eventPublisher.publishOrderCancelled(order.getId(), performanceId, seatIds, userId);

                throw new BusinessException("PAYMENT_FAILED", "Pago rechazado: " + payment.getStatusDetail(), HttpStatus.BAD_REQUEST);
            }

            return new YapePaymentResponse(
                    payment.getId(),
                    payment.getStatus(),
                    payment.getStatusDetail(),
                    request.amount(),
                    request.description()
            );

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) { // Capturamos errores generales de MP para cancelar
            order.setStatus(OrderStatus.CANCELLED);
            ordersRepository.save(order);
            eventPublisher.publishOrderCancelled(order.getId(), performanceId, seatIds, userId);

            throw new BusinessException("MP_ERROR", "Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public OrdersResponse getOrderById(Long id) {
        Orders order = ordersRepository.findByIdWithItems(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Orden no encontrada", HttpStatus.NOT_FOUND));
        return orderMapper.toDto(order, null);
    }

    @Override
    public List<OrdersResponse> getOrdersByUserId(Long userId) {
        return ordersRepository.findByUserId(userId).stream()
                .map(order -> orderMapper.toDto(order, null))
                .toList();
    }

    @Override
    @Transactional
    public OrdersResponse refundOrder(Long id) {
        // 1. Buscar la orden con sus items en una sola query (evita LazyInitializationException)
        Orders order = ordersRepository.findByIdWithItems(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "La orden no existe", HttpStatus.NOT_FOUND));

        // 2. Validar que la orden esté pagada para poder reembolsarla
        if (order.getStatus() != OrderStatus.PAID) {
            throw new BusinessException("BAD_REQUEST", "Solo se pueden reembolsar órdenes con estado PAID", HttpStatus.BAD_REQUEST);
        }

        // 3. Cambiar estado a REFUNDED
        order.setStatus(OrderStatus.REFUNDED);
        Orders savedOrder = ordersRepository.save(order);

        // 4. Publicar evento para liberar asientos en el microservicio de Inventory
        // Enviamos una lista de IDs de asientos para que el otro servicio los ponga como AVAILABLE
        List<Long> seatIds = (order.getItems() != null)
                ? order.getItems().stream().map(OrderItem::getSeatId).toList()
                : List.of();


        // 5. Retornar la respuesta usando el mapper que ya arreglamos
        return orderMapper.toDto(savedOrder, null);
    }

    // Configuracion de RabbitMQ



    private void updateOrderStatus(Long orderId, OrderStatus status) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "No se encontró la orden con ID: " + orderId, HttpStatus.NOT_FOUND));

        order.setStatus(status);
        ordersRepository.save(order);
    }

    /**
     * Extrae el claim "uid" (Long) del JWT de la sesión actual.
     * El claim "sub" contiene el email del usuario — NO usarlo para identificar el userId.
     */
    private Long extractUidFromJwt() {
        JwtAuthenticationToken authentication =
                (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = authentication.getToken();
        Object uid = jwt.getClaim("uid");
        if (uid instanceof Number) {
            return ((Number) uid).longValue();
        }
        return Long.parseLong(uid.toString());
    }
}
