package com.recco.order.service.exception;

public class OrderNotAllowedException extends RuntimeException{
	 public OrderNotAllowedException(String message) {
	        super(message);
	    }
}
