package com.recco.order.service.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recco.order.service.dto.OrderItemRequest;
import com.recco.order.service.dto.OrderRequest;
import com.recco.order.service.entity.Order;
import com.recco.order.service.entity.OrderItem;
import com.recco.order.service.exception.MenuItemNotFoundException;
import com.recco.order.service.exception.OrderEditNotAllowedException;
import com.recco.order.service.exception.OrderNotFoundException;
import com.recco.order.service.feign.MenuServiceClient;
import com.recco.order.service.repository.OrderRepository;
import com.recco.order.service.util.OrderStatus;
import com.recco.order.service.util.TableStatus;

import feign.FeignException;

@Service
public class UserOrderService {
    
	private static final Logger logger = LogManager.getLogger(UserOrderService.class); 
	
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private MenuServiceClient menuServiceClient;

    @Transactional
    public Order placeOrder(OrderRequest request, String qrToken) {
    	
    	logger.info("placeOrder start");
    	
        String tableId = request.getTableId();
        
        // 1. Get all orders for this table sorted by creation date (newest first)
        List<Order> tableOrders = orderRepository.findByTableIdOrderByCreatedAtDesc(tableId);
        
        if (!tableOrders.isEmpty()) {
            Order latestOrder = tableOrders.get(0); // Most recent order
            
            // 2. Check if table is ACTIVE (has pending order)
            if (latestOrder.getTableStatus() == TableStatus.ACTIVE) {
                if (latestOrder.getOrderStatus() == OrderStatus.PENDING) {
                    // Add to existing active order
                    return updateOrderWithNewItems(latestOrder, request, qrToken);
                } else {
                    throw new OrderEditNotAllowedException(
                        "Cannot add to order with status: " + latestOrder.getOrderStatus());
                }
            }
        }
        
        logger.info("placeOrder end");
        
        // 3. Create new order with ACTIVE table status and items
        return createNewOrderWithItems(request, qrToken);
    }

    private Order createNewOrderWithItems(OrderRequest request, String qrToken) {
    	
    	logger.info("createNewOrderWithItems start");
    	
        Order order = new Order();
        order.setTableId(request.getTableId());
        order.setOrderStatus(OrderStatus.PENDING);
        order.setTotalAmount(0.0);
        order.setCreatedAt(LocalDateTime.now());
        order.setTableStatus(TableStatus.ACTIVE);
        
        // Initialize items list
        order.setItems(new ArrayList<>());
        
        // Save the order first to generate ID
        logger.info("request: " + request);
        logger.info("order: " + order);
        Order savedOrder = orderRepository.save(order);
        
        // Add all items from request
        for (OrderItemRequest itemRequest : request.getItems()) {
            OrderItem item = new OrderItem();
            item.setMenuItemName(itemRequest.getMenuItemName());
            item.setQuantity(itemRequest.getQuantity());
            item.setPrice(getItemPrice(itemRequest.getMenuItemName(), qrToken));
            item.setOrder(savedOrder); // Set the bidirectional relationship
            savedOrder.getItems().add(item);
        }
        
        // Calculate total
        savedOrder.setTotalAmount(calculateOrderTotal(savedOrder));
        System.out.println("savedOrder: " + savedOrder);
        logger.info("createNewOrderWithItems end");
        return orderRepository.save(savedOrder);
    }

    private Order updateOrderWithNewItems(Order order, OrderRequest request, String qrToken) {
        // Existing item update logic
        for (OrderItemRequest itemRequest : request.getItems()) {
            Optional<OrderItem> existingItem = order.getItems().stream()
                    .filter(item -> item.getMenuItemName().equals(itemRequest.getMenuItemName()))
                    .findFirst();

            if (existingItem.isPresent()) {
                existingItem.get().setQuantity(existingItem.get().getQuantity() + itemRequest.getQuantity());
            } else {
                OrderItem newItem = new OrderItem();
                newItem.setMenuItemName(itemRequest.getMenuItemName());
                newItem.setQuantity(itemRequest.getQuantity());
                newItem.setPrice(getItemPrice(itemRequest.getMenuItemName(), qrToken));
                newItem.setOrder(order);
                order.getItems().add(newItem);
            }
        }
        
        order.setTotalAmount(calculateOrderTotal(order));
        return orderRepository.save(order);
    }


    public Order editOrder(Long orderId, OrderRequest updatedOrderRequest, String qrToken) {
        Order existingOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));
                
        if (existingOrder.getOrderStatus() != OrderStatus.PENDING) {
            throw new OrderEditNotAllowedException(
                String.format("Order %d is %s. Only PENDING orders can be edited by users",
                    orderId, existingOrder.getOrderStatus()));
        }

        // Clear existing items and add new ones
        existingOrder.getItems().clear();
        
        List<OrderItem> newItems = updatedOrderRequest.getItems().stream()
                .map(itemReq -> {
                    OrderItem item = new OrderItem();
                    item.setMenuItemName(itemReq.getMenuItemName());
                    item.setQuantity(itemReq.getQuantity());
                    item.setPrice(getItemPrice(itemReq.getMenuItemName(), qrToken));
                    item.setOrder(existingOrder);
                    return item;
                })
                .collect(Collectors.toList());
                
        existingOrder.getItems().addAll(newItems);
        existingOrder.setTotalAmount(calculateOrderTotal(existingOrder));
        
        return orderRepository.save(existingOrder);
    }

    public Order getActiveOrder(String tableId) {
        return orderRepository.findByTableIdOrderByCreatedAtDesc(tableId)
                .stream()
                .filter(order -> order.getTableStatus() == TableStatus.ACTIVE)
                .findFirst()
                .orElse(null);
    }

    public List<Order> getOrderHistory(String tableId) {
        return orderRepository.findByTableIdAndOrderStatusNot(tableId, OrderStatus.PENDING);
    }
//
//    @Transactional
//    public void cancelOrder(Long orderId, String qrToken) {
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new OrderNotFoundException("Order not found"));
//                
//        if (order.getOrderStatus() != OrderStatus.PENDING) {
//            throw new OrderEditNotAllowedException(
//                String.format("Cannot cancel order %d with status %s", 
//                    orderId, order.getOrderStatus()));
//        }
//        
//        order.setOrderStatus(OrderStatus.CANCELLED);
//        orderRepository.save(order);
//    }
//
    public Double getItemPrice(String itemName, String qrToken) {
        try {
            return menuServiceClient.getItemPrice(itemName, qrToken);
        } catch (FeignException.NotFound ex) {
            throw new MenuItemNotFoundException("Menu item not found: " + itemName);
        }
    }

    private double calculateOrderTotal(Order order) {
        return order.getItems().stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
    }
}