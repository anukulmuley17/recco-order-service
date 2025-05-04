package com.recco.order.service.exception;

public class OrderEditNotAllowedException extends RuntimeException {
    public OrderEditNotAllowedException(String message) {
        super(message);
    }
}
