package com.example.demo.service;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.OrderStatusLog;
import com.example.demo.entity.Product;
import com.example.demo.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统定时任务服务
 * - 超时订单自动取消（pending_payment 超过30分钟未支付），含库存恢复
 * - 骑手接单超时未取餐自动释放回订单池
 * - 配送超时自动标记异常
 */
@Service
public class ScheduledTaskService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskService.class);

    /** 超时阈值：30分钟 */
    private static final int PAYMENT_TIMEOUT_MINUTES = 30;

    /** 骑手接单后超时未取餐阈值：15分钟 */
    private static final int PICKUP_TIMEOUT_MINUTES = 15;

    /** 配送超时阈值：60分钟 */
    private static final int DELIVERY_TIMEOUT_MINUTES = 60;

    /** 骑手可被释放的订单状态集合（已接单但未取餐/配送中） */
    private static final List<String> GRABBED_BUT_NOT_DELIVERING = List.of("preparing", "prepared");

    private final OrderRepository orderRepository;
    private final OrderStatusLogRepository statusLogRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    public ScheduledTaskService(OrderRepository orderRepository,
                                 OrderStatusLogRepository statusLogRepository,
                                 OrderItemRepository orderItemRepository,
                                 ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.statusLogRepository = statusLogRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
    }

    // ==================== 1. 支付超时自动取消（含库存恢复） ====================

    /**
     * 每分钟扫描一次，将超过30分钟仍为 pending_payment 的订单自动取消，并恢复库存
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoCancelExpiredOrders() {
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(PAYMENT_TIMEOUT_MINUTES);
        List<Order> expiredOrders = orderRepository
                .findByOrderStatusAndCreatedAtBefore("pending_payment", expireTime);

        if (expiredOrders.isEmpty()) {
            return;
        }

        for (Order order : expiredOrders) {
            String oldStatus = order.getOrderStatus();
            order.setOrderStatus("cancelled");
            orderRepository.save(order);

            // 恢复库存（与 OrderService.cancelOrder 行为一致）
            restoreStock(order.getId());

            // 写入状态日志
            OrderStatusLog statusLog = new OrderStatusLog(
                    order.getId(), oldStatus, "cancelled",
                    null, "超过" + PAYMENT_TIMEOUT_MINUTES + "分钟未支付，系统自动取消（库存已恢复）"
            );
            statusLogRepository.save(statusLog);
        }

        log.info("定时任务：超时自动取消 {} 笔订单（超时阈值={}分钟），已恢复所有商品库存",
                expiredOrders.size(), PAYMENT_TIMEOUT_MINUTES);
    }

    // ==================== 2. 骑手接单超时未取餐自动释放 ====================

    /**
     * 每2分钟扫描一次，骑手接单超过15分钟未取餐的订单，自动释放骑手并回退状态
     * 条件：订单状态为 preparing/prepared，且 updatedAt 超过15分钟
     */
    @Scheduled(fixedRate = 120000)
    @Transactional
    public void autoReleaseExpiredGrabbedOrders() {
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(PICKUP_TIMEOUT_MINUTES);

        // 查找骑手已接单但长时间未取餐/配送中的订单
        List<Order> grabbedOrders = orderRepository.findByOrderStatusInAndUpdatedAtBefore(
                GRABBED_BUT_NOT_DELIVERING, expireTime);

        if (grabbedOrders.isEmpty()) {
            return;
        }

        for (Order order : grabbedOrders) {
            if (order.getRiderId() == null) continue;

            Integer riderId = order.getRiderId();
            String oldStatus = order.getOrderStatus();

            // 释放骑手，回退到 prepared 状态（重新进入可抢单池）
            order.setRiderId(null);
            order.setOrderStatus("prepared");
            orderRepository.save(order);

            OrderStatusLog statusLog = new OrderStatusLog(
                    order.getId(), oldStatus, "prepared",
                    null, "骑手接单超过" + PICKUP_TIMEOUT_MINUTES + "分钟未取餐，系统自动释放回订单池"
            );
            statusLogRepository.save(statusLog);

            log.info("定时任务：释放超时未取餐订单 orderId={}, 原riderId={}, 状态{}→prepared",
                    order.getId(), riderId, oldStatus);
        }

        log.info("定时任务：共释放 {} 笔超时未取餐订单", grabbedOrders.size());
    }

    // ==================== 3. 配送超时自动标记异常 ====================

    /**
     * 每5分钟扫描一次，配送中超过60分钟的订单，标记为异常
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void autoMarkDeliveryTimeout() {
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(DELIVERY_TIMEOUT_MINUTES);
        List<Order> timeoutOrders = orderRepository
                .findByOrderStatusAndCreatedAtBefore("delivering", expireTime);

        if (timeoutOrders.isEmpty()) {
            return;
        }

        for (Order order : timeoutOrders) {
            String oldStatus = order.getOrderStatus();
            order.setOrderStatus("exception");
            orderRepository.save(order);

            OrderStatusLog statusLog = new OrderStatusLog(
                    order.getId(), oldStatus, "exception",
                    null, "配送超过" + DELIVERY_TIMEOUT_MINUTES + "分钟，系统自动标记为异常"
            );
            statusLogRepository.save(statusLog);
        }

        log.warn("定时任务：共标记 {} 笔配送超时异常订单（超时阈值={}分钟）",
                timeoutOrders.size(), DELIVERY_TIMEOUT_MINUTES);
    }

    // ==================== 内部工具方法 ====================

    /**
     * 恢复订单商品的库存（供取消/超时等场景复用）
     */
    private void restoreStock(Integer orderId) {
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : orderItems) {
            productRepository.findById(item.getProductId()).ifPresent(product -> {
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
            });
        }
    }
}
