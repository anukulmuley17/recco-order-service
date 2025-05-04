package com.recco.order.service.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recco.order.service.dto.OrderItemRequest;
import com.recco.order.service.dto.OrderRequest;
import com.recco.order.service.entity.Order;
import com.recco.order.service.entity.OrderItem;
import com.recco.order.service.exception.MenuItemNotFoundException;
import com.recco.order.service.exception.OrderEditNotAllowedException;
import com.recco.order.service.feign.MenuServiceClient;
import com.recco.order.service.repository.OrderItemRepository;
import com.recco.order.service.repository.OrderRepository;
import com.recco.order.service.util.OrderStatus;

import feign.FeignException;

@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;
//    @Autowired
//    private OrderItemRepository orderItemRepository;
    @Autowired
    private MenuServiceClient menuServiceClient;
    
//    @Transactional
//    public Order placeOrder(OrderRequest request, String qrToken) {
//        // Fetch active (PENDING) orders for the table
//        List<Order> activeOrders = orderRepository.findByTableIdAndStatus(request.getTableId(), OrderStatus.PENDING);
//
//        Order order;
//        if (activeOrders.isEmpty()) {
//            // 1. First save the order with empty items
//            order = new Order();
//            order.setTableId(request.getTableId());
//            order.setStatus(OrderStatus.PENDING);
//            order.setTotalAmount(0.0);
//            order = orderRepository.save(order);
//        } else {
//            // Use the first active order
//            order = activeOrders.get(0);
//        }
//
//        double totalAmount = order.getTotalAmount();
//        List<OrderItem> itemsToSave = new ArrayList<>();
//
//        for (OrderItemRequest itemRequest : request.getItems()) {
//            OrderItem existingItem = null;
//            
//            // Only try to find existing item if order has been persisted
//            if (order.getOrderId() != null) {
//                existingItem = orderItemRepository.findByOrderAndMenuItemName(order, itemRequest.getItemName());
//            }
//
//            if (existingItem != null) {
//                // Increase quantity if the item is already in the order
//                existingItem.setQuantity(existingItem.getQuantity() + itemRequest.getQuantity());
//                itemsToSave.add(existingItem);
//            } else {
//                // Add new item to the order
//                OrderItem newItem = new OrderItem();
//                newItem.setOrder(order);
//                newItem.setMenuItemName(itemRequest.getItemName());
//                newItem.setQuantity(itemRequest.getQuantity());
//
//                // Get price from Menu Service
//                Double itemPrice = getPriceOfItem(itemRequest.getItemName(), qrToken);
//                newItem.setPrice(itemPrice);
//                itemsToSave.add(newItem);
//            }
//
//            totalAmount += (itemsToSave.get(itemsToSave.size() - 1).getPrice() * itemRequest.getQuantity());
//        }
//
//        // Save all items
//        orderItemRepository.saveAll(itemsToSave);
//
//        // Update total amount and save order
//        order.setTotalAmount(totalAmount);
//        return orderRepository.save(order);
//    }
    
    @Transactional
    public Order placeOrder(OrderRequest request, String qrToken) {
        // Fetch active (PENDING) orders for the table
        List<Order> activeOrders = orderRepository.findByTableIdAndOrderStatus(request.getTableId(), OrderStatus.PENDING);

        Order order;
        if (activeOrders.isEmpty()) {
            // No active order, create a new one
            order = new Order();
            order.setTableId(request.getTableId());
            order.setOrderStatus(OrderStatus.PENDING);
            order.setTotalAmount(0.0);
            order.setItems(new ArrayList<>());
            order = orderRepository.save(order);
        } else {
            // Use the first active order
            order = activeOrders.get(0);
            // Ensure items are initialized (if not loaded)
            if (order.getItems() == null) {
                order.setItems(new ArrayList<>());
            }
        }

        // Process each item in the request
        for (OrderItemRequest itemRequest : request.getItems()) {
            OrderItem existingItem = findItemInOrder(order, itemRequest.getItemName());

            if (existingItem != null) {
                // Update existing item quantity
                existingItem.setQuantity(existingItem.getQuantity() + itemRequest.getQuantity());
            } else {
                // Create new item
                existingItem = new OrderItem();
                existingItem.setOrder(order);
                existingItem.setMenuItemName(itemRequest.getItemName());
                existingItem.setQuantity(itemRequest.getQuantity());
                
                // Get price from Menu Service
                Double itemPrice = getPriceOfItem(itemRequest.getItemName(), qrToken);
                existingItem.setPrice(itemPrice);
                
                // Add to order's items collection
                order.getItems().add(existingItem);
            }
        }

        // Recalculate total amount from scratch for all items
        double totalAmount = order.getItems().stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();

        // Update total amount
        order.setTotalAmount(totalAmount);
        
        // Save will cascade to items
        return orderRepository.save(order);
    }
    
    private OrderItem findItemInOrder(Order order, String itemName) {
        return order.getItems().stream()
                .filter(item -> item.getMenuItemName().equals(itemName))
                .findFirst()
                .orElse(null);
    }
    
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        if (order.getOrderStatus() == OrderStatus.APPROVED) {
            throw new RuntimeException("Approved orders cannot be canceled.");
        }
        System.out.println("Canceling order - Current status: {}" + order.getOrderStatus());
        order.setOrderStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }
    
    public List<Order> getPastOrdersByTableId(String tableId) {
        return orderRepository.findByTableIdAndOrderStatusNot(tableId, OrderStatus.PENDING);
    }

    public List<Order> getOrdersByTableId(String tableId) {
        return orderRepository.findByTableId(tableId);
    }

    public Order updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setOrderStatus(status);
        return orderRepository.save(order);
    }
    
    public Double getPriceOfItem(String itemName, String qrToken) {
        try {
            return menuServiceClient.getItemPrice(itemName, qrToken); //call menu service
        } catch (FeignException.NotFound ex) {
            throw new MenuItemNotFoundException("Menu item not found: " + itemName);
        }
    }

    
//    public Order editOrder(Long orderId, OrderRequest updatedOrderRequest) {
//        Order existingOrder = orderRepository.findById(orderId)
//                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
//        
//        if (existingOrder.getStatus() == OrderStatus.CANCELLED) {
//            throw new OrderEditNotAllowedException("CANCELLED order cannot be edited");
//        }
//        
//        // Update items by converting from OrderItemRequest to OrderItem
////        List<OrderItem> updatedItems = convertToOrderItems(updatedOrderRequest.getItems());
//        List<OrderItem> updatedItems = convertToOrderItems(updatedOrderRequest.getItems());
//        
//        existingOrder.setItems(updatedItems);
//
//        // Recalculate total amount based on updated items
////        double updatedTotal = calculateTotalAmount(updatedOrderRequest.getItems());
//        double updatedTotal = calculateTotalAmount(updatedOrderRequest.getItems());
//        existingOrder.setTotalAmount(updatedTotal);
//
//        // You can also reset status if needed or add more fields update logic
//        existingOrder.setStatus(OrderStatus.PENDING);  // Optional: mark edited orders as pending
//
//        return orderRepository.save(existingOrder);
//    }
    
    public Order editOrder(Long orderId, OrderRequest updatedOrderRequest, String qrToken) {
        Order existingOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        if (existingOrder.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new OrderEditNotAllowedException("CANCELLED order cannot be edited");
        }

        // Prepare updated items
        List<OrderItem> updatedItems = updatedOrderRequest.getItems().stream().map(req -> {
            OrderItem item = new OrderItem();
            item.setMenuItemName(req.getItemName());
            item.setQuantity(req.getQuantity());

            // Fetch price from Menu Service (trusted source)
            Double price = getPriceOfItem(req.getItemName(), qrToken);
            if (price == null) {
                throw new RuntimeException("Price not found for item: " + req.getItemName());
            }

            item.setPrice(price);
            item.setOrder(existingOrder); // Maintain bidirectional relationship
            return item;
        }).collect(Collectors.toList());

        existingOrder.getItems().clear();               // Clear old items
        existingOrder.getItems().addAll(updatedItems);  // Add new items

        // Recalculate total based on fetched prices
        double updatedTotal = updatedItems.stream()
                .mapToDouble(item -> item.getQuantity() * item.getPrice())
                .sum();
        existingOrder.setTotalAmount(updatedTotal);

        existingOrder.setOrderStatus(OrderStatus.PENDING);   // Reset status

        return orderRepository.save(existingOrder);
    }




    
//    private List<OrderItem> convertToOrderItems(List<OrderItemRequest> itemRequests) {
//        return itemRequests.stream().map(req -> {
//            OrderItem item = new OrderItem();
//            item.setMenuItemName(req.getItemName());
//            item.setQuantity(req.getQuantity());
//            item.setPrice(req.getPrice());
//            return item;
//        }).toList();
//    }
//
//    private double calculateTotalAmount(List<OrderItemRequest> items) {
//        return items.stream()
//                .mapToDouble(item -> item.getPrice() * item.getQuantity())
//                .sum();
//    }
    
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }


}
