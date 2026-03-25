package com.teleticket.domain.repository;

import com.teleticket.domain.entity.PaymentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentLogRepository extends JpaRepository<PaymentLog, Long> {
}
