package com.teleticket.application.client;

import com.teleticket.application.dto.AvailabilityResponseDTO;
import com.teleticket.config.security.FeignSecurityConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "inventory-ms",
        url = "${app.inventory.base-url}",
        configuration = FeignSecurityConfig.class
)
public interface InventoryClient {

    @GetMapping("/availability/{performanceId}")
    AvailabilityResponseDTO getAvailability(@PathVariable("performanceId") Long performanceId); //
    // Nuevo: Obtener precio por zona y función
    @GetMapping("/inventory/pricing/{performanceId}/{zoneId}")
    Double getPrice(@PathVariable Long performanceId, @PathVariable Long zoneId);
}
