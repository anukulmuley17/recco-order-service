package com.recco.order.service.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.recco.order.service.entity.Order;
import com.recco.order.service.util.OrderStatus;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByTableId(String tableId);
    List<Order>  findByTableIdAndStatus(String tableId, OrderStatus status);
	List<Order> findByTableIdAndStatusNot(String tableId, OrderStatus status);
	@Query("SELECT o FROM Order o WHERE o.status = 'PENDING' AND o.createdAt < :time")
    List<Order> findPendingOrdersBefore(LocalDateTime time);
}

