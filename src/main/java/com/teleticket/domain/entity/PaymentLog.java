package com.teleticket.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Orders order;

    @Column(name = "transaction_id") // El ID que viene de Mercado Pago
    private String transactionId;

    @Column(name = "payment_method") // "yape"
    private String paymentMethod;

    private Double amount;

    private String status; // "approved", "rejected", etc.

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}