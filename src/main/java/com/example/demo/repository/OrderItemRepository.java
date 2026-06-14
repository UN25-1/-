package com.example.demo.repository;

import com.example.demo.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 订单明细数据访问层
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {

    /** 查询某订单的所有明细 */
    List<OrderItem> findByOrderId(Integer orderId);

    /** 批量查询多个订单的所有明细 */
    List<OrderItem> findByOrderIdIn(List<Integer> orderIds);
}
