package com.recco.order.service.controller;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.recco.order.service.dto.OrderRequest;
import com.recco.order.service.entity.Order;
import com.recco.order.service.services.UserOrderService;
import com.recco.order.service.util.TableIdDecoder;

@RestController
@RequestMapping("/api/user/order")
@CrossOrigin("http://localhost:4200/")
public class UserOrderController {

    private static final Logger logger = LogManager.getLogger(UserOrderController.class);

    @Autowired
    private UserOrderService orderService;

    @Autowired 
    private TableIdDecoder tableIdDecoder;

    @PostMapping("/place")
    public ResponseEntity<Order> placeOrder(@RequestBody OrderRequest request,
            @RequestParam(value = "qrToken", required = true) String qrToken) {
        String tableId = tableIdDecoder.decodeTableId(qrToken);
        logger.info("Received order request from table: {}", tableId);
        request.setTableId(tableId); // Set tableId from decoded qrToken
        return ResponseEntity.ok(orderService.placeOrder(request, qrToken));
    }

    @PutMapping("/edit-pending")
    public ResponseEntity<Order> editOrder(
            @RequestParam Long orderId,
            @RequestBody OrderRequest updatedOrderRequest,
            @RequestParam(value = "qrToken", required = true) String qrToken) {
        String tableId = tableIdDecoder.decodeTableId(qrToken);
        logger.info("Editing order {} for table: {}", orderId, tableId);
        updatedOrderRequest.setTableId(tableId);
        return ResponseEntity.ok(orderService.editOrder(orderId, updatedOrderRequest, qrToken));
    }

    @GetMapping("/active-order")
    public ResponseEntity<?> getMyOrders(
            @RequestParam(value = "qrToken", required = true) String qrToken) {
        String tableId = tableIdDecoder.decodeTableId(qrToken);
        logger.info("Fetching active orders for table: {}", tableId);
        
        Order activeOrder = orderService.getActiveOrder(tableId);
        
        if (activeOrder == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No active order found. Please place a new order.");
        }
        
        return ResponseEntity.ok(activeOrder);
    }
}