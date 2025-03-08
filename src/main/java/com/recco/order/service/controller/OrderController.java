package com.recco.order.service.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.recco.order.service.dto.OrderRequest;
import com.recco.order.service.entity.Order;
import com.recco.order.service.services.OrderService;
import com.recco.order.service.util.OrderStatus;

@RestController
@RequestMapping("/api/order")

public class OrderController {
	@Autowired
    private OrderService orderService;

    @PostMapping("/place")
    public ResponseEntity<Order> placeOrder(@RequestBody OrderRequest request) {
        return ResponseEntity.ok(orderService.placeOrder(request));
    }

    @GetMapping("/status")
    public ResponseEntity<List<Order>> getOrders(@RequestParam String tableId) {
        return ResponseEntity.ok(orderService.getOrdersByTableId(tableId));
    }

    @PutMapping("/update")
    public ResponseEntity<Order> updateOrder(@RequestParam Long orderId, @RequestParam OrderStatus status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, status));
    }
    
    @GetMapping("/item-price")
    public Double fetchPrice(@RequestParam("name") String name) {
        return orderService.getPriceOfItem(name);
    }
    
    @DeleteMapping("/cancel")
    public ResponseEntity<String> cancelOrder(@RequestParam Long orderId) {
        orderService.cancelOrder(orderId);
        return ResponseEntity.ok("Order with ID " + orderId + " has been canceled.");
    }
    
    @GetMapping("/history")
    public ResponseEntity<List<Order>> getOrderHistory(@RequestParam String tableId) {
        return ResponseEntity.ok(orderService.getPastOrdersByTableId(tableId));
    }
}

