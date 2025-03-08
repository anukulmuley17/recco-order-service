package com.recco.order.service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.recco.order.service.entity.Order;
import com.recco.order.service.entity.OrderItem;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

	OrderItem findByOrderAndMenuItemName(Order order, String name);

}