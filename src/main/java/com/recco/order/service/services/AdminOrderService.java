package com.recco.order.service.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recco.order.service.dto.OrderItemRequest;
import com.recco.order.service.dto.OrderRequest;
import com.recco.order.service.entity.Order;
import com.recco.order.service.entity.OrderItem;
import com.recco.order.service.exception.MenuItemNotFoundException;
import com.recco.order.service.exception.OrderEditNotAllowedException;
import com.recco.order.service.exception.OrderNotAllowedException;
import com.recco.order.service.exception.OrderNotFoundException;
import com.recco.order.service.feign.MenuServiceClient;
import com.recco.order.service.repository.OrderRepository;
import com.recco.order.service.repository.TableRepository;
import com.recco.order.service.util.OrderStatus;
import com.recco.order.service.util.TableStatus;

import feign.FeignException;

@Service
public class AdminOrderService {
	
	private static final Logger logger = LogManager.getLogger(AdminOrderService.class); 
    
    @Autowired
    private TableRepository tableRepository;
    
    private final OrderRepository orderRepository;
    private final MenuServiceClient menuServiceClient;
    
    public AdminOrderService(OrderRepository orderRepository, MenuServiceClient menuServiceClient) {
        this.orderRepository = orderRepository;
        this.menuServiceClient = menuServiceClient;
    }
    
//    @Transactional
//    public Order placeOrderAsAdmin(OrderRequest request, String tableId) {
//        Order order = new Order();
//        order.setTableId(tableId);
//        order.setOrderStatus(OrderStatus.PENDING);
//        order.setTableStatus(TableStatus.ACTIVE);
//        order.setTotalAmount(0.0);
//        order.setCreatedAt(LocalDateTime.now());
//        
//        Order savedOrder = orderRepository.save(order);
//        return updateOrderItems(savedOrder, request.getItems(),tableId);
//    }
    
    @Transactional
    public Order placeOrderAsAdmin(OrderRequest request, String tableId) {
    	
    	logger.info("placeOrderAsAdmin start");
        
        // 1. Get all orders for this table sorted by creation date (newest first)
        List<Order> tableOrders = orderRepository.findByTableIdOrderByCreatedAtDesc(tableId);
        
        if (!tableOrders.isEmpty()) {
            Order latestOrder = tableOrders.get(0); // Most recent order
            
            // 2. Check if table is ACTIVE (has pending order)
            if (latestOrder.getTableStatus() == TableStatus.ACTIVE) {
//                if (latestOrder.getOrderStatus() == OrderStatus.PENDING) {
                    // Add to existing active order
                    return updateOrderWithNewItems(latestOrder, request, tableId);
                }
//            else if(latestOrder.getTableStatus() == TableStatus.INACTIVE) {
////                    throw new OrderEditNotAllowedException(
////                        "Cannot add to order with status: " + latestOrder.getOrderStatus());
//                    throw new OrderNotAllowedException(
//                            "Cannot order with status: INACTIVE to table: " + latestOrder.getTableId());
//                }
//            }
        }
        
        logger.info("placeOrderAsAdmin end");
        
        // 3. Create new order with ACTIVE table status and items
        return createNewOrderWithItems(request, tableId);
    }
    
    @Transactional
    public Order editOrderAsAdmin(Long orderId, OrderRequest request,String tableId) {
    	logger.info("editOrderAsAdmin start");
    	logger.info("tableId: " + tableId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));
        
        if (order.getOrderStatus() == OrderStatus.REJECTED) {
            throw new IllegalStateException("Cannot edit " + order.getOrderStatus() + " orders");
        }
        logger.info("order: " + order);
//        order.getItems().clear();
        orderRepository.flush(); 
        Order updatedOrder = updateOrderItems(order, request.getItems(),tableId);
        logger.info("updatedOrder: " + updatedOrder);
        logger.info("editOrderAsAdmin end");
//        return updateOrderItems(order, request.getItems(),tableId);
        return updatedOrder;
    }
    
    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));
        
        order.setOrderStatus(status);
        return orderRepository.save(order);
    }
    
    @Transactional
    public Order updateTableStatus(String tableId, TableStatus tableStatus, Long orderId) {
        Order order = orderId != null 
                ? orderRepository.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException("Order not found"))
                : orderRepository.findFirstByTableIdOrderByCreatedAtDesc(tableId)
                    .orElseThrow(() -> new OrderNotFoundException("No orders found for table"));
        
        if (!order.getTableId().equals(tableId)) {
            throw new IllegalStateException("Order does not belong to specified table");
        }
        
        order.setTableStatus(tableStatus);
        return orderRepository.save(order);
    }
    
    @Transactional
    public void cancelOrderAsAdmin(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));
        
        order.setOrderStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }
    
    public Page<Order> getTableHistory(String tableId, int page, int size) {
        // Validate page and size
        if (page < 0) {
            throw new IllegalArgumentException("Page index must not be less than zero");
        }
        if (size < 1) {
            throw new IllegalArgumentException("Page size must not be less than one");
        }

        // Create page request with sorting
        PageRequest pageRequest = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return orderRepository.findByTableId(tableId, pageRequest);
    }
    
    private Order updateOrderItems(Order order, List<OrderItemRequest> itemRequests,String tableId) {
        logger.info("order: " + order + ", itemRequests: " + itemRequests);
    	order.setItems(itemRequests.stream()
                .map(itemReq -> {
                    OrderItem item = new OrderItem();
                    item.setMenuItemName(itemReq.getMenuItemName());
                    item.setQuantity(itemReq.getQuantity());
                    item.setPrice(getItemPrice(itemReq.getMenuItemName(),tableId));
                    item.setOrder(order);
                    
                    return item;
                })
                .collect(Collectors.toList()));
        
        order.setTotalAmount(calculateOrderTotal(order));
        return orderRepository.save(order);
    }
    
    private Order updateOrderWithNewItems(Order order, OrderRequest request, String tableId) {
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
                newItem.setPrice(getItemPrice(itemRequest.getMenuItemName(), tableId));
                newItem.setOrder(order);
                order.getItems().add(newItem);
            }
        }
        
        order.setTotalAmount(calculateOrderTotal(order));
        return orderRepository.save(order);
    }
    
    public Double getItemPrice(String itemName, String tableId) {
        try {
        	logger.info("getItemPrice start");
        	logger.info("itemName: " + itemName + " ,tableId: " + tableId);
            Double price = menuServiceClient.getItemPriceAdmin(itemName, tableId);
            logger.info("getItemPrice start");
            return price;
        } catch (FeignException.NotFound ex) {
            throw new MenuItemNotFoundException("Menu item not found: " + itemName);
        }
    }
    
    private double calculateOrderTotal(Order order) {
        return order.getItems().stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
    }
    
    public List<String> getAllTableIds() {
        return tableRepository.findAllTableIds();
    }
    
    private Order createNewOrderWithItems(OrderRequest request, String tableId) {
    	
    	logger.info("createNewOrderWithItems start");
    	logger.info("tableId: " + tableId);
    	//validate tableId from list of table else throw INVALIDTABLE EXCEPTION
        Order order = new Order();
        order.setTableId(tableId);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setTotalAmount(0.0);
        order.setCreatedAt(LocalDateTime.now());
        order.setTableStatus(TableStatus.ACTIVE);
        
        // Initialize items list
        order.setItems(new ArrayList<>());
        
        // Save the order first to generate ID
        logger.info("order" + order);
        
        Order savedOrder = orderRepository.save(order);
        
        // Add all items from request
        for (OrderItemRequest itemRequest : request.getItems()) {
            OrderItem item = new OrderItem();
            item.setMenuItemName(itemRequest.getMenuItemName());
            item.setQuantity(itemRequest.getQuantity());
            item.setPrice(getItemPrice(itemRequest.getMenuItemName(), tableId));
            item.setOrder(savedOrder); // Set the bidirectional relationship
            savedOrder.getItems().add(item);
        }
        
        // Calculate total
        savedOrder.setTotalAmount(calculateOrderTotal(savedOrder));
        System.out.println("savedOrder: " + savedOrder);
        logger.info("createNewOrderWithItems end");
        return orderRepository.save(savedOrder);
    }

	public Optional<Order> getLatestOrderByTableId(String tableId) {
//	    List<Order> orders = orderRepository.findByTableIdAndOrderId(tableId, orderId);
	    Optional<Order> order = orderRepository.findFirstByTableIdOrderByOrderIdDesc(tableId);
//	    if (orders == null || orders.isEmpty()) {
//	        throw new OrderNotFoundException("Order not found for tableId: " + tableId + " and orderId: " + orderId);
//	}
	    if(order.isEmpty()) {
	    	throw new OrderNotFoundException("Order not found for tableId: " + tableId );
	    }
	return order;
}

	public Optional<Order> getOrderByTableIdAndOrderId(String tableId, Long orderId) {
		// TODO Auto-generated method stub
		Optional<Order> order = orderRepository.findFirstByTableIdAndOrderId(tableId, orderId);
		
		if(order.isEmpty()) {
	    	throw new OrderNotFoundException("Order not found for tableId: " + tableId + " and orderId: " + orderId );
	    }
		
		return order;
	}
}
