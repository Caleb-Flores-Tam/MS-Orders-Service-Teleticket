package com.teleticket.application.client;

import com.teleticket.config.security.FeignSecurityConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(
        name = "user-ms",
        url = "${app.usuario.base-url}",
        configuration = FeignSecurityConfig.class
)
public interface UsuarioClient {

    @GetMapping("/auth/users/{id}")
    UserResponse get(Long id);
}
