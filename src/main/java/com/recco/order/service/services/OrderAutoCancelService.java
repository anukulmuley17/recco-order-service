package com.recco.order.service.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recco.order.service.entity.Order;
import com.recco.order.service.repository.OrderRepository;
import com.recco.order.service.util.OrderStatus;
import com.recco.order.service.util.TableStatus;

@Service
public class OrderAutoCancelService {

    private final OrderRepository orderRepository;

    public OrderAutoCancelService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Scheduled(fixedRate = 60000) // Runs every 60 seconds
    @Transactional
    public void autoCancelOrders() {
        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
        
        List<Order> expiredOrders = orderRepository.findPendingOrdersBefore(tenMinutesAgo);

        if (!expiredOrders.isEmpty()) {
            expiredOrders.forEach(order -> {
            	
            order.setOrderStatus(OrderStatus.REJECTED);
            order.setTableStatus(TableStatus.INACTIVE);}
        );
            orderRepository.saveAll(expiredOrders);
            System.out.println("Auto-canceled " + expiredOrders.size() + " expired orders.");
        }
    }
}

//todo if order is rejected then set table status as INACTIVE and item status as CANCELLED
