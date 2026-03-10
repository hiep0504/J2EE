package com.example.Backend_J2EE.repository;

import com.example.Backend_J2EE.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    List<Order> findByAccount_Id(Integer accountId);

    List<Order> findByAccount_IdOrderByOrderDateDesc(Integer accountId);

    List<Order> findByStatus(Order.OrderStatus status);

    List<Order> findAllByOrderByOrderDateDesc();
}
