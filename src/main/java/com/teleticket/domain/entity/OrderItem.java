package com.teleticket.domain.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "order_items")
@Data
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Orders order;

    @Column(name = "seat_id")
    private Long seatId;

    @Column(name = "performance_id")
    private Long performanceId;

    @Column(name = "unit_price")
    private Double unitPrice;

}
