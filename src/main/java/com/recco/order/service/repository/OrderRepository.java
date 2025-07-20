package com.recco.order.service.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.recco.order.service.entity.Order;
import com.recco.order.service.util.OrderStatus;
import com.recco.order.service.util.TableStatus;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByTableId(String tableId);
    List<Order> findByTableIdAndOrderId(String tableId,Long orderId);
    Optional<Order> findFirstByTableIdAndOrderId(String tableId,Long orderId);
    Optional<Order> findFirstByTableIdOrderByOrderIdDesc(String tableId);
    List<Order>  findByTableIdAndOrderStatus(String tableId, OrderStatus orderStatus);
	List<Order> findByTableIdAndOrderStatusNot(String tableId, OrderStatus orderStatus);
	@Query("SELECT o FROM Order o WHERE o.orderStatus = 'PENDING' AND o.createdAt < :time")
    List<Order> findPendingOrdersBefore(LocalDateTime time);
	List<Order> findByTableIdAndTableStatus(String tableId, TableStatus tableStatus);
	List<Order> findByTableIdOrderByCreatedAtDesc(String tableId);
	Optional<Order> findFirstByTableIdOrderByCreatedAtDesc(String tableId);
	Page<Order> findByTableId(String tableId, Pageable pageable);
}

