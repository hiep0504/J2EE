package com.example.Backend_J2EE.repository;

import com.example.Backend_J2EE.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {

    List<OrderDetail> findByOrder_Id(Integer orderId);

    @Modifying
    @Transactional
    void deleteByOrder_Id(Integer orderId);
}
