package com.recco.order.service.exception;

public class MenuItemNotFoundException extends RuntimeException {
    public MenuItemNotFoundException(String message) {
        super(message);
    }
}

