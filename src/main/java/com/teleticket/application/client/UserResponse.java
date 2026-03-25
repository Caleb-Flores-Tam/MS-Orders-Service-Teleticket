package com.teleticket.application.client;

import lombok.Builder;

import java.util.List;

@Builder
public record UserResponse(
        Long id,
        String email,
        String status,
        List<String> roles) {
}
