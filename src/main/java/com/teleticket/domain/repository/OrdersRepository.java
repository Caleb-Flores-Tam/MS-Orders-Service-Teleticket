package com.teleticket.domain.repository;

import com.teleticket.domain.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrdersRepository extends JpaRepository<Orders, Long> {

    @Query("SELECT o FROM Orders o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<Orders> findByIdWithItems(@Param("id") Long id);

    Optional<Orders> findById(Long id);

    List<Orders> findByUserId(Long userId);
}
