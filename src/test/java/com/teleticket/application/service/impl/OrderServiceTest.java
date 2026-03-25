package com.teleticket.application.service.impl;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.resources.payment.Payment;
import com.teleticket.application.client.InventoryClient;
import com.teleticket.application.client.UserResponse;
import com.teleticket.application.dto.AvailabilityResponseDTO;
import com.teleticket.application.dto.CheckoutRequest;
import com.teleticket.application.dto.SeatAvailabilityDTO;
import com.teleticket.application.mapper.OrderMapper;
import com.teleticket.domain.entity.OrderItem;
import com.teleticket.domain.entity.OrderStatus;
import com.teleticket.domain.entity.Orders;
import com.teleticket.domain.entity.PaymentLog;
import com.teleticket.domain.repository.OrderItemRepository;
import com.teleticket.domain.repository.OrdersRepository;
import com.teleticket.domain.repository.PaymentLogRepository;
import com.teleticket.web.dto.Request.OrdersRequest;
import com.teleticket.web.dto.Response.OrdersResponse;
import com.teleticket.web.exception.InventoryException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrdersRepository ordersRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private InventoryClient inventoryClient;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private PaymentLogRepository paymentLogRepository;
    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    @DisplayName("Debe crear una orden exitosamente cuando los asientos están disponibles")
    void createOrderSuccess() {
        // 1. PREPARACIÓN (Given)
        mockSecurityContext("1");
        // Request completo para evitar nulos en la lógica del service
        OrdersRequest request = OrdersRequest.builder()
                .userId(1L)
                .performanceId(100L)
                .seatIds(List.of(10L))
                .totalAmount(118.0)
                .taxAmount(18.0)
                .build();

        // Simulación de respuesta del inventario (Disponibilidad)
        SeatAvailabilityDTO seat = SeatAvailabilityDTO.builder()
                .seatId(10L)
                .status("AVAILABLE")
                .zoneId(5L)
                .build();

        AvailabilityResponseDTO mockAvailability = AvailabilityResponseDTO.builder()
                .performanceId(100L)
                .seats(List.of(seat))
                .build();

        // Simulación de respuesta del microservicio de Usuario
        UserResponse mockUser = UserResponse.builder()
                .id(1L)
                .email("calebflorestambracc@gmail.com") // Usando tu correo de contacto
                .status("ACTIVE")
                .build();

        // Entidad Mock de Orders con datos completos para evitar errores de validación
        Orders mockOrder = Orders.builder()
                .id(500L)
                .userId(1L)
                .totalAmount(118.0)
                .taxAmount(18.0)
                .status(OrderStatus.PENDING)
                .build();

        // Respuesta esperada del Mapper DTO
        OrdersResponse mockResponse = OrdersResponse.builder()
                .id(500L)
                .user_id(1L)
                .email("calebflorestambracc@gmail.com")
                .totalAmount(118.0)
                .taxAmount(18.0)
                .status("PENDING")
                .build();

        // Configuración de comportamientos de los Mocks
        when(inventoryClient.getAvailability(100L)).thenReturn(mockAvailability);
        when(inventoryClient.getPrice(100L, 5L)).thenReturn(100.0);
        when(orderMapper.toEntity(any(OrdersRequest.class))).thenReturn(mockOrder);
        when(ordersRepository.save(any(Orders.class))).thenReturn(mockOrder);
        when(orderMapper.toDto(any(Orders.class), any())).thenReturn(mockResponse);

        // 2. EJECUCIÓN (When)
        OrdersResponse response = orderService.createOrder(request);

        // 3. VERIFICACIÓN (Then)
        assertNotNull(response, "La respuesta no debería ser nula");
        assertEquals(500L, response.id());
        assertEquals("calebflorestambracc@gmail.com", response.email());

        // Verificamos que se llamó al inventario para disponibilidad y precio
        verify(inventoryClient).getAvailability(100L);
        verify(inventoryClient).getPrice(100L, 5L);

        // Verificamos las interacciones con la base de datos
        // Se guarda 2 veces en el service: una inicial y otra para actualizar montos reales
        verify(ordersRepository, times(2)).save(any(Orders.class));
        verify(orderItemRepository).save(any(OrderItem.class));

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Debe lanzar InventoryException si el asiento está ocupado")
    void createOrderFailsWhenNotAvailable() {

        mockSecurityContext("1");
        // Preparación con asiento SOLD
        OrdersRequest request = OrdersRequest.builder()
                .performanceId(1L)
                .seatIds(List.of(10L))
                .build();

        SeatAvailabilityDTO seat = SeatAvailabilityDTO.builder()
                .seatId(10L)
                .status("SOLD") // Simulamos que ya se vendió
                .build();

        AvailabilityResponseDTO mockAvailability = AvailabilityResponseDTO.builder()
                .seats(List.of(seat))
                .build();

        when(inventoryClient.getAvailability(1L)).thenReturn(mockAvailability);

        // Ejecución y Verificación de la Excepción
        assertThrows(InventoryException.class, () -> orderService.createOrder(request));

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Debe lanzar InventoryException si el precio obtenido es nulo o inválido")
    void createOrderFailsWhenPriceIsInvalid() {
        mockSecurityContext("1");
        // 1. PREPARACIÓN (Given)
        OrdersRequest request = OrdersRequest.builder()
                .userId(1L)
                .performanceId(100L)
                .seatIds(List.of(10L))
                .build();

        // Simulamos que el asiento está disponible, pero pertenece a una zona específica
        SeatAvailabilityDTO seat = SeatAvailabilityDTO.builder()
                .seatId(10L)
                .status("AVAILABLE")
                .zoneId(5L)
                .build();

        AvailabilityResponseDTO mockAvailability = AvailabilityResponseDTO.builder()
                .seats(List.of(seat))
                .build();

        Orders mockOrder = Orders.builder()
                .id(500L)
                .userId(1L)
                .build();

        when(inventoryClient.getAvailability(100L)).thenReturn(mockAvailability);
        when(orderMapper.toEntity(any(OrdersRequest.class))).thenReturn(mockOrder);
        when(ordersRepository.save(any(Orders.class))).thenReturn(mockOrder);

        // Simulamos que el inventario devuelve NULL para el precio de esa zona
        when(inventoryClient.getPrice(100L, 5L)).thenReturn(null);

        // 2. EJECUCIÓN Y VERIFICACIÓN (When & Then)
        InventoryException exception = assertThrows(InventoryException.class, () ->
                orderService.createOrder(request)
        );

        // Verificamos que el mensaje de error sea el correcto
        assertTrue(exception.getMessage().contains("No se pudo obtener el precio") ||
                exception.getMessage().contains("Error al obtener el precio"));

        // Verificamos que no se intentó guardar la orden por segunda vez (rollback lógico)
        verify(orderItemRepository, never()).save(any());
        assertThrows(InventoryException.class, () -> orderService.createOrder(request));

        SecurityContextHolder.clearContext();
    }

    /*
    @Test
    @DisplayName("Debe procesar el pago con Yape exitosamente")
    void processYapePaymentSuccess() {
        // 1. PREPARACIÓN (Given)

        // Simulación de Usuario Autenticado (Evita NullPointerException en SecurityContext)
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("1"); // ID del usuario como String
        SecurityContextHolder.getContext().setAuthentication(auth);

        // DTO de entrada ajustado al Record CheckoutRequest(Long orderId, String token, String email)
        CheckoutRequest checkoutReq = new CheckoutRequest(
                500L,
                "token_de_prueba_yape",
                "calebft30@gmail.com"
        );

        // Entidad Mock de la orden en estado PENDING
        Orders mockOrder = Orders.builder()
                .id(500L)
                .userId(1L)
                .totalAmount(118.0)
                .status(OrderStatus.PENDING)
                .build();

        // DTO de respuesta esperado
        OrdersResponse mockResponse = OrdersResponse.builder()
                .id(500L)
                .status("PAID")
                .totalAmount(118.0)
                .build();

        // Configuración de Mocks de Repositorios y Mappers
        when(ordersRepository.findById(500L)).thenReturn(Optional.of(mockOrder));
        when(ordersRepository.save(any(Orders.class))).thenReturn(mockOrder);
        when(orderMapper.toDto(any(Orders.class), any())).thenReturn(mockResponse);

        // 2. EJECUCIÓN (When)
        // Utilizamos MockedConstruction para interceptar "new PaymentClient()"
        try (MockedConstruction<PaymentClient> mocked = mockConstruction(PaymentClient.class,
                (mock, context) -> {
                    Payment mockPayment = mock(Payment.class);
                    when(mockPayment.getStatus()).thenReturn("approved");
                    when(mockPayment.getId()).thenReturn(123456789L);
                    // Programamos el mock para que devuelva un pago aprobado al llamar a create()
                    when(mock.create(any(PaymentCreateRequest.class))).thenReturn(mockPayment);
                })) {

            OrdersResponse response = orderService.processYapePayment(checkoutReq);

            // 3. VERIFICACIÓN (Then)
            assertNotNull(response);
            assertEquals(500L, response.id()); // Uso de .id() si es Record o .getId() si es Clase
            assertEquals("PAID", response.status());

            // Verificamos que se guardó el log de auditoría en la base de datos
            verify(paymentLogRepository).save(any(PaymentLog.class));

            // Verificamos que la orden se actualizó a PAID
            verify(ordersRepository, atLeastOnce()).save(argThat(order ->
                    order.getStatus() == OrderStatus.PAID));
        } finally {
            // Limpiamos el contexto de seguridad para no afectar a otros tests
            SecurityContextHolder.clearContext();
        }
    }
    */

    private void mockSecurityContext(String userId) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(userId);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }



    // PRUEBA DE TOKEN DE MERCADO LIBRE
/*
    @Test
    @DisplayName("Validar Conexión Real con Mercado Pago Sandbox")
    void testRealConnectionWithMercadoPago() {
        // 1. Configurar el contexto de seguridad (necesario por tu lógica de OrderServiceImpl)
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("1");
        SecurityContextHolder.getContext().setAuthentication(auth);

        // 2. Mockear solo la base de datos local para que no falle al buscar la orden
        Orders mockOrder = Orders.builder()
                .id(500L)
                .userId(1L)
                .totalAmount(10.0) // Monto pequeño para prueba
                .status(OrderStatus.PENDING)
                .build();

        when(ordersRepository.findById(500L)).thenReturn(Optional.of(mockOrder));
        when(ordersRepository.save(any())).thenReturn(mockOrder);

        // 3. Preparar el pedido con un TOKEN DE PRUEBA de tarjeta (suministrado por MP)
        // Nota: El "paymentToken" normalmente viene del front, pero usaremos uno de prueba
        CheckoutRequest checkoutReq = new CheckoutRequest(
                500L,
                "ff8080814c116563014c14c74d0d0006", // Aquí iría el token generado con la tarjeta de la imagen 6dd4d8
                "calebft30@gmail.com"
        );

        // 4. Ejecutar (Aquí se usará tu @Value mpAccessToken real)
        assertDoesNotThrow(() -> {
            orderService.processYapePayment(checkoutReq);
        });
    }
*/
}