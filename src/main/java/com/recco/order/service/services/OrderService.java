package com.recco.order.service.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recco.order.service.dto.OrderItemRequest;
import com.recco.order.service.dto.OrderRequest;
import com.recco.order.service.entity.Order;
import com.recco.order.service.entity.OrderItem;
import com.recco.order.service.feign.MenuServiceClient;
import com.recco.order.service.repository.OrderItemRepository;
import com.recco.order.service.repository.OrderRepository;
import com.recco.order.service.util.OrderStatus;

@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private MenuServiceClient menuServiceClient;

    @Transactional
    public Order placeOrder(OrderRequest request) {
        // Fetch active (PENDING) orders for the table
        List<Order> activeOrders = orderRepository.findByTableIdAndStatus(request.getTableId(), OrderStatus.PENDING);

        Order order;
        if (activeOrders.isEmpty()) {
            // No active order, create a new one
            order = new Order();
            order.setTableId(request.getTableId());
            order.setSessionId(request.getSessionId());
            order.setStatus(OrderStatus.PENDING);
            order.setTotalAmount(0.0);
            order = orderRepository.save(order);
        } else {
            // Use the first active order
            order = activeOrders.get(0);
        }

        double totalAmount = order.getTotalAmount();

        for (OrderItemRequest itemRequest : request.getItems()) {
            OrderItem existingItem = orderItemRepository.findByOrderAndMenuItemName(order, itemRequest.getName());

            if (existingItem != null) {
                // ✅ Increase quantity if the item is already in the order
                existingItem.setQuantity(existingItem.getQuantity() + itemRequest.getQuantity());
            } else {
                // ✅ Add new item to the order
                existingItem = new OrderItem();
                existingItem.setOrder(order);
                existingItem.setMenuItemName(itemRequest.getName());
                existingItem.setQuantity(itemRequest.getQuantity());

                // Get price from Menu Service
                Double itemPrice = getPriceOfItem(itemRequest.getName());
                existingItem.setPrice(itemPrice);
            }

            totalAmount += existingItem.getPrice() * itemRequest.getQuantity();
            orderItemRepository.save(existingItem);
        }

        // Update total amount
        order.setTotalAmount(totalAmount);
        return orderRepository.save(order);
    }

    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        if (order.getStatus() == OrderStatus.APPROVED) {
            throw new RuntimeException("Approved orders cannot be canceled.");
        }
        System.out.println("Canceling order - Current status: {}" + order.getStatus());
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }
    
    public List<Order> getPastOrdersByTableId(String tableId) {
        return orderRepository.findByTableIdAndStatusNot(tableId, OrderStatus.PENDING);
    }

    public List<Order> getOrdersByTableId(String tableId) {
        return orderRepository.findByTableId(tableId);
    }

    public Order updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(status);
        return orderRepository.save(order);
    }
    
    public Double getPriceOfItem(String itemName) {
        return menuServiceClient.getItemPrice(itemName); // Calls Menu Service API
    }
}
