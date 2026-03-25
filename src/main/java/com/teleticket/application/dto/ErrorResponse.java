package com.teleticket.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ErrorResponse {
    private String code;
    private String message;
    private int status;
    private LocalDateTime timestamp;
}
