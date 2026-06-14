package com.example.demo.repository;

import com.example.demo.entity.OrderStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 订单状态日志数据访问层
 */
@Repository
public interface OrderStatusLogRepository extends JpaRepository<OrderStatusLog, Integer> {

    /** 查询某订单的所有状态变更记录（按时间正序） */
    List<OrderStatusLog> findByOrderIdOrderByCreatedAtAsc(Integer orderId);
}
