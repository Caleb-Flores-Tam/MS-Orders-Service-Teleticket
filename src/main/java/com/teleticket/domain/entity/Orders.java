package com.teleticket.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "total_amount")
    private Double totalAmount;

    @Column(name = "tax_amount")
    private Double taxAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Este método se ejecuta automáticamente antes de insertar en la DB
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<PaymentLog> paymentLogs;

}
