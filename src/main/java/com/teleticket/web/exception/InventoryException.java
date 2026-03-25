package com.teleticket.web.exception;

import org.springframework.http.HttpStatus;

public class InventoryException extends BusinessException {
    public InventoryException(String message) {
        super("INVENTORY_ERROR", message, HttpStatus.CONFLICT);
    }
}
