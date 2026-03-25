package com.teleticket.web.exception;

import com.teleticket.application.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Maneja errores específicos de Feign (comunicación entre microservicios)
    @ExceptionHandler(feign.FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(feign.FeignException e) {
        String message = "Error de comunicación con el servicio externo";

        if (e.status() == 404) message = "El recurso solicitado en el otro servicio no existe.";
        if (e.status() == 401) message = "No autorizado para consultar el servicio externo.";

        ErrorResponse error = ErrorResponse.builder()
                .code("EXTERNAL_SERVICE_ERROR")
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(e.status() > 0 ? e.status() : 500).body(error);
    }

    // Maneja errores de negocio (como el que lanzamos con throw new RuntimeException)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        ErrorResponse error = ErrorResponse.builder()
                .code("BUSINESS_ERROR")
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
   }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorResponse error = ErrorResponse.builder()
                .code(e.getCode())
                .message(e.getMessage())
                .status(e.getStatus().value())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, e.getStatus());
    }

    @ExceptionHandler(InventoryException.class)
    public ResponseEntity<ErrorResponse> handleInventoryException(InventoryException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .code("INVENTORY_ERROR")
                .message(ex.getMessage())
                .status(400)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.badRequest().body(error);
    }

}
