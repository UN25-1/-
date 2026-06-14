package com.example.demo.repository;

import com.example.demo.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 支付数据访问层
 * 所有查询均使用 JPA 参数化查询，防止 SQL 注入
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    /**
     * 根据订单ID查找支付记录（一个订单仅一条支付记录）
     */
    Optional<Payment> findByOrderId(Integer orderId);

    /**
     * 批量查询订单的支付记录
     */
    List<Payment> findByOrderIdIn(List<Integer> orderIds);

    /**
     * 根据用户关联的订单ID列表查询支付记录（按支付时间倒序）
     */
    List<Payment> findByOrderIdInOrderByPaidAtDesc(List<Integer> orderIds);
}
