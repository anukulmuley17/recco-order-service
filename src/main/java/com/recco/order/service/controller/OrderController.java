package com.recco.order.service.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
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
import com.recco.order.service.util.TableIdDecoder;

@RestController
@RequestMapping("/api/order")
@CrossOrigin("http://localhost:4200/")
public class OrderController {
	@Autowired
    private OrderService orderService;
	
	@Autowired TableIdDecoder tableIdDecoder;
    
	// ✅ Extract tableId from qrToken
	private String extractTableId(String qrToken) {
		String tableId = tableIdDecoder.decodeTableId(qrToken);
		return tableId;

	}
	
    @PostMapping("/place")
    public ResponseEntity<Order> placeOrder(@RequestBody OrderRequest request,
    		@RequestParam(value = "qrToken", required = true) String qrToken) {
    	
    	String tableId = extractTableId(qrToken);
    	System.out.println("Received order request from table: " + tableId);
    	System.out.println("request: " + request);
    	request.setTableId(tableId);
    	System.out.println("request: " + request);
        return ResponseEntity.ok(orderService.placeOrder(request,qrToken));

    }

    @GetMapping("/fetch-order")
    public ResponseEntity<List<Order>> getOrders(@RequestParam(value = "qrToken", required = true) String qrToken) {
    	String tableId = extractTableId(qrToken);
    	System.out.println("Checking status for table: " + tableId);
        return ResponseEntity.ok(orderService.getOrdersByTableId(tableId));
    }
    
    @GetMapping("/get-order")
    public ResponseEntity<Order> getOrderById(
            @RequestParam Long orderId,
            @RequestParam(value = "qrToken", required = true) String qrToken) {
        String tableId = extractTableId(qrToken);
        System.out.println("Fetching order with ID: " + orderId + " for table: " + tableId);
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    @PutMapping("/update-status")
    public ResponseEntity<Order> updateOrderStatus(@RequestParam Long orderId, @RequestParam OrderStatus status,
    		@RequestParam(value = "qrToken", required = true) String qrToken) {
    	String tableId = extractTableId(qrToken);
    	System.out.println("Updating order for table: " + tableId);
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, status));
    }
    
    @PutMapping("/edit")
    public ResponseEntity<Order> editOrder(@RequestParam Long orderId,
            @RequestBody OrderRequest updatedOrderRequest,
            @RequestParam(value = "qrToken", required = true) String qrToken) {
        String tableId = extractTableId(qrToken);
        System.out.println("Editing order for table: " + tableId);
        updatedOrderRequest.setTableId(tableId);
        return ResponseEntity.ok(orderService.editOrder(orderId, updatedOrderRequest, qrToken));
    }

    
    @GetMapping("/item-price")
    public Double fetchPrice(@RequestParam("name") String name,
    		@RequestParam(value = "qrToken", required = true) String qrToken) {
    	String tableId = extractTableId(qrToken);
    	System.out.println("Fetching price of item:" + name + " for table: " + tableId);
        return orderService.getPriceOfItem(name,qrToken);
    }
    
    @DeleteMapping("/cancel")
    public ResponseEntity<String> cancelOrder(@RequestParam Long orderId
    		,@RequestParam(value = "qrToken", required = true) String qrToken) {
    	String tableId = extractTableId(qrToken);
    	System.out.println("Cancelling order for table: " + tableId);
        orderService.cancelOrder(orderId);
        return ResponseEntity.ok("Order with ID " + orderId + " has been canceled.");
    }
    
    @GetMapping("/history")
    public ResponseEntity<List<Order>> getOrderHistory(@RequestParam(value = "qrToken", required = true) String qrToken) {
    	String tableId = extractTableId(qrToken);
    	System.out.println("Getting order history for table: " + tableId);
        return ResponseEntity.ok(orderService.getPastOrdersByTableId(tableId));
    }
}

