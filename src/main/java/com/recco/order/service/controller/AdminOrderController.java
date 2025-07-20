package com.recco.order.service.controller;

import org.springframework.data.domain.Page;
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
import com.recco.order.service.exception.OrderNotFoundException;
import com.recco.order.service.services.AdminOrderService;
import com.recco.order.service.util.OrderStatus;
import com.recco.order.service.util.TableIdDecoder;
import com.recco.order.service.util.TableStatus;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@RestController
@CrossOrigin("http://localhost:4200/")
//@CrossOrigin("*")
@RequestMapping("/api/admin/order")
public class AdminOrderController {
    
	private static final Logger logger = LogManager.getLogger(AdminOrderController.class);
	
    private final AdminOrderService adminOrderService;
    private final TableIdDecoder tableIdDecoder;
    
    public AdminOrderController(AdminOrderService adminOrderService, TableIdDecoder tableIdDecoder) {
        this.adminOrderService = adminOrderService;
        this.tableIdDecoder = tableIdDecoder;
    }
    
    @PostMapping("/place")
    public ResponseEntity<Order> placeOrderAsAdmin(
            @RequestBody OrderRequest request,
//            @RequestParam String qrToken,
            @RequestParam String tableId
            ) {
//        String tableId = tableIdDecoder.decodeTableId(qrToken);
        logger.info("Received order request from table: {}", tableId);
        return ResponseEntity.ok(adminOrderService.placeOrderAsAdmin(request, tableId));
    }
    
    @PutMapping("/edit")
    public ResponseEntity<Order> editOrderAsAdmin(
            @RequestParam Long orderId,
            @RequestBody OrderRequest request,
            @RequestParam String tableId) {
        return ResponseEntity.ok(adminOrderService.editOrderAsAdmin(orderId, request,tableId));
    }
    
//    @GetMapping("/fetch-latest-order")
//    public ResponseEntity<Optional<Order>> getOrders(@RequestParam(value = "tableId", required = true) String tableId
////    		@RequestParam(value = "orderId", required = false) Long orderId
//    		) {
//  
//    	System.out.println("Checking status for table: " + tableId);
//        return ResponseEntity.ok(adminOrderService.getLatestOrderByTableId(tableId));
//    }
    
    @GetMapping("/fetch-latest-order")
    public ResponseEntity<?> getOrders(
            @RequestParam(value = "tableId", required = true) String tableId,
            @RequestParam(value = "orderId", required = false) Long orderId) {

        try {
            if (orderId != null) {
                // Service throws exception if not found
                Optional<Order> order = adminOrderService.getOrderByTableIdAndOrderId(tableId, orderId);
                
                if (order.isPresent()) {
                    return ResponseEntity.ok(order.get());
                }
                return ResponseEntity.ok(order);
            } else {
                // Handle latest order case with proper Optional handling
                Optional<Order> latestOrder = adminOrderService.getLatestOrderByTableId(tableId);
                if (latestOrder.isPresent()) {
                    return ResponseEntity.ok(latestOrder.get());
                }
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No orders found for tableId: " + tableId);
            }
        } catch (OrderNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred");
        }
    }
    
    @PutMapping("/update-order-status")
    public ResponseEntity<Order> updateOrderStatus(
            @RequestParam Long orderId,
            @RequestParam OrderStatus status
//            @RequestParam String tableId
            ) {
        return ResponseEntity.ok(adminOrderService.updateOrderStatus(orderId, status));
    }
    
    @PutMapping("/update-table-status")
    public ResponseEntity<Order> updateTableStatus(
            @RequestParam String tableId,
            @RequestParam TableStatus tableStatus,
            @RequestParam(required = false) Long orderId) {
        return ResponseEntity.ok(adminOrderService.updateTableStatus(tableId, tableStatus, orderId));
    }
    
    @PutMapping("/cancel")
    public ResponseEntity<String> cancelOrderAsAdmin(
            @RequestParam Long orderId) {
        adminOrderService.cancelOrderAsAdmin(orderId);
        return ResponseEntity.ok("Order cancelled successfully");
    }
    
    @GetMapping("/table-history")
    public ResponseEntity<Page<Order>> getTableHistory(
//            @RequestParam String qrToken,
    		@RequestParam String tableId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
//        String tableId = tableIdDecoder.decodeTableId(qrToken);
        return ResponseEntity.ok(adminOrderService.getTableHistory(tableId, page, size));
    }
    
    @GetMapping("/table-list")
    public ResponseEntity<List<String>> getTableList() {
        return ResponseEntity.ok(adminOrderService.getAllTableIds());
    }
    
}