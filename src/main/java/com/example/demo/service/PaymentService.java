package com.example.demo.service;

import com.example.demo.dto.PaymentResponse;
import com.example.demo.entity.*;
import com.example.demo.exception.BusinessException;
import com.example.demo.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 支付服务 —— 发起支付 + 模拟支付回调 + 退款处理 + 支付查询
 * 所有数据库操作均通过 JPA 参数化查询执行，防止 SQL 注入
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    /** 货到付款无需在线支付，可直接视为支付确认 */
    private static final String CASH_ON_DELIVERY = "cash";

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusLogRepository statusLogRepository;
    private final MerchantDetailRepository merchantDetailRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    public PaymentService(PaymentRepository paymentRepository,
                          OrderRepository orderRepository,
                          OrderStatusLogRepository statusLogRepository,
                          MerchantDetailRepository merchantDetailRepository,
                          ProductRepository productRepository,
                          OrderItemRepository orderItemRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.statusLogRepository = statusLogRepository;
        this.merchantDetailRepository = merchantDetailRepository;
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
    }

    // ==================== 发起支付 ====================

    /**
     * 发起支付：为订单创建支付记录
     * 
     * 业务规则：
     * - 仅订单归属用户可操作
     * - 订单状态必须为 pending_payment
     * - 一个订单只能有一条支付记录（payments.order_id UNIQUE 约束保证）
     * - 货到付款(cash)：直接标记支付成功，无需等待网关回调
     *
     * @param userId   当前登录用户ID
     * @param orderId  订单ID
     * @param payMethod 支付方式（wechat/alipay/card/cash）
     * @return 支付记录响应
     */
    @Transactional
    public PaymentResponse createPayment(Integer userId, Integer orderId, String payMethod) {
        // 1. 校验订单归属
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该订单");
        }

        // 2. 校验订单状态
        if (!"pending_payment".equals(order.getOrderStatus())) {
            throw new BusinessException(400, "当前订单状态[" + order.getOrderStatus() + "]不允许发起支付，仅待支付订单可操作");
        }

        // 3. 校验是否已存在支付记录（UNIQUE 约束兜底）
        paymentRepository.findByOrderId(orderId).ifPresent(p -> {
            throw new BusinessException(400, "该订单已存在支付记录，状态：" + PaymentResponse.payStatusDesc(p.getPayStatus()));
        });

        // 4. 创建支付记录
        Payment payment = new Payment(orderId, payMethod, order.getTotalAmount());
        payment = paymentRepository.save(payment);

        // 5. 货到付款：直接标记支付完成
        if (CASH_ON_DELIVERY.equals(payMethod)) {
            markPaymentSuccess(payment, order, userId);
        }

        log.info("支付记录创建：paymentId={}, orderId={}, payMethod={}, amount={}",
                payment.getId(), orderId, payMethod, order.getTotalAmount());

        return buildPaymentResponse(payment, order);
    }

    // ==================== 模拟支付回调 ====================

    /**
     * 模拟支付网关回调：支付成功
     * 
     * 真实场景中由支付网关（微信/支付宝）异步回调触发。
     * 当前模拟实现：生成虚拟交易流水号，标记支付成功并推进订单状态。
     *
     * @param paymentId 支付记录ID
     * @param userId    当前登录用户ID（用于记录操作人）
     * @return 支付记录响应
     */
    @Transactional
    public PaymentResponse simulatePayCallback(Integer paymentId, Integer userId) {
        // 1. 校验支付记录
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(404, "支付记录不存在"));

        if (!"pending".equals(payment.getPayStatus())) {
            throw new BusinessException(400, "当前支付状态[" + PaymentResponse.payStatusDesc(payment.getPayStatus())
                    + "]不允许支付，仅待支付记录可操作");
        }

        // 2. 校验订单
        Order order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new BusinessException(404, "关联订单不存在"));
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该支付记录");
        }
        if (!"pending_payment".equals(order.getOrderStatus())) {
            throw new BusinessException(400, "订单状态异常：期望 pending_payment，实际 " + order.getOrderStatus());
        }

        // 3. 标记支付成功
        markPaymentSuccess(payment, order, userId);

        log.info("模拟支付回调成功：paymentId={}, orderId={}, transactionNo={}",
                paymentId, order.getId(), payment.getTransactionNo());

        return buildPaymentResponse(payment, order);
    }

    /**
     * 快捷支付：一键创建支付记录 + 模拟回调（旧接口兼容）
     * 默认使用微信支付
     */
    @Transactional
    public PaymentResponse quickPay(Integer orderId, Integer userId) {
        // 先检查是否已有支付记录
        Optional<Payment> existing = paymentRepository.findByOrderId(orderId);
        if (existing.isPresent()) {
            Payment p = existing.get();
            if ("pending".equals(p.getPayStatus())) {
                // 已有待支付记录，直接触发回调
                if (CASH_ON_DELIVERY.equals(p.getPayMethod())) {
                    throw new BusinessException(400, "货到付款订单无需重复支付");
                }
                return simulatePayCallback(p.getId(), userId);
            }
            throw new BusinessException(400, "该订单已有支付记录，状态："
                    + PaymentResponse.payStatusDesc(p.getPayStatus()));
        }
        // 创建支付记录（默认微信支付）并模拟回调
        PaymentResponse resp = createPayment(userId, orderId, "wechat");
        // 微信支付需要模拟回调
        return simulatePayCallback(resp.getId(), userId);
    }

    // ==================== 退款处理 ====================

    /**
     * 处理退款：将已支付的订单退款
     * 
     * 场景：订单取消时自动触发，或管理员手动退款。
     * 流转：success → refunding → refunded
     * 货到付款(cash)订单无需退款（未实际收款）
     *
     * @param orderId 订单ID
     * @param userId  操作人ID
     * @param reason  退款原因
     * @return 支付记录响应
     */
    @Transactional
    public PaymentResponse processRefund(Integer orderId, Integer userId, String reason) {
        // 1. 查询支付记录
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElse(null);

        if (payment == null) {
            log.info("订单无支付记录，跳过退款：orderId={}", orderId);
            return null;
        }

        // 货到付款无需退款
        if (CASH_ON_DELIVERY.equals(payment.getPayMethod())) {
            log.info("货到付款订单无需退款：orderId={}", orderId);
            return buildPaymentResponse(payment, null);
        }

        // 仅支付成功状态可退款
        if (!"success".equals(payment.getPayStatus())) {
            log.info("支付状态非 success，跳过退款：orderId={}, payStatus={}", orderId, payment.getPayStatus());
            return buildPaymentResponse(payment, null);
        }

        // 2. 标记退款中
        payment.setPayStatus("refunding");
        paymentRepository.save(payment);

        // 3. 模拟退款回调（真实场景由支付网关异步回调）
        payment.setPayStatus("refunded");
        payment.setRefundedAt(LocalDateTime.now());
        // 生成退款流水号
        payment.setTransactionNo(payment.getTransactionNo() + "_REFUND");
        paymentRepository.save(payment);

        // 4. 写入订单状态日志
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            writeStatusLog(orderId, null, "refunded", userId, reason != null ? reason : "订单取消自动退款");
        }

        log.info("退款处理完成：orderId={}, paymentId={}, amount={}", orderId, payment.getId(), payment.getPaidAmount());

        return buildPaymentResponse(payment, order);
    }

    // ==================== 支付查询 ====================

    /**
     * 查询用户的支付记录（分页）
     */
    public Page<PaymentResponse> getMyPayments(Integer userId, Pageable pageable) {
        // 1. 查询用户所有订单ID
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (orders.isEmpty()) {
            return Page.empty();
        }

        List<Integer> orderIds = orders.stream()
                .map(Order::getId)
                .collect(Collectors.toList());

        // 2. 查询支付记录，按支付时间倒序
        List<Payment> payments = paymentRepository.findByOrderIdInOrderByPaidAtDesc(orderIds);

        // 3. 构建 orderId → Order 映射
        Map<Integer, Order> orderMap = orders.stream()
                .collect(Collectors.toMap(Order::getId, o -> o));

        // 4. 构建响应列表
        List<PaymentResponse> responses = payments.stream()
                .map(p -> buildPaymentResponse(p, orderMap.get(p.getOrderId())))
                .collect(Collectors.toList());

        // 5. 手动分页
        return manualPage(responses, pageable);
    }

    /**
     * 查询单条支付详情（含所有权校验）
     */
    public PaymentResponse getPaymentDetail(Integer paymentId, Integer userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(404, "支付记录不存在"));

        Order order = orderRepository.findById(payment.getOrderId()).orElse(null);
        if (order != null && !order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权查看该支付记录");
        }
        return buildPaymentResponse(payment, order);
    }

    /**
     * 根据订单ID查询支付记录（含所有权校验）
     */
    public PaymentResponse getPaymentByOrderId(Integer orderId, Integer userId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(404, "该订单暂无支付记录"));

        if (payment.getOrderId() != null) {
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order != null && !order.getUserId().equals(userId)) {
                throw new BusinessException(403, "无权查看该支付记录");
            }
        }
        Order order = orderRepository.findById(orderId).orElse(null);
        return buildPaymentResponse(payment, order);
    }

    // ==================== 内部工具方法 ====================

    /**
     * 标记支付成功
     * 更新支付记录 + 推进订单状态：pending_payment → pending
     */
    private void markPaymentSuccess(Payment payment, Order order, Integer userId) {
        // 更新支付记录
        payment.setPayStatus("success");
        payment.setTransactionNo(generateTransactionNo(payment.getPayMethod()));
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // 推进订单状态
        String oldStatus = order.getOrderStatus();
        order.setOrderStatus("pending");
        orderRepository.save(order);

        // 支付成功后扣减库存（原子操作，防止超卖）
        deductOrderStock(order);

        // 写入状态日志
        writeStatusLog(order.getId(), oldStatus, "pending", userId,
                PaymentResponse.payMethodDesc(payment.getPayMethod()) + "支付成功，交易号：" + payment.getTransactionNo());
    }

    /**
     * 扣减订单中所有商品的库存（原子操作，防止超卖）
     * 任一商品库存不足时抛出 BusinessException(409)，事务回滚
     */
    private void deductOrderStock(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        for (OrderItem item : items) {
            int affected = productRepository.decrementStock(item.getProductId(), item.getQuantity());
            if (affected == 0) {
                String productName = productRepository.findById(item.getProductId())
                        .map(Product::getName)
                        .orElse("ID:" + item.getProductId());
                throw new BusinessException(409, "商品[" + productName
                        + "]库存不足，支付失败，请稍后重试");
            }
        }
    }

    /**
     * 生成模拟交易流水号
     * 格式：PAY_{支付方式}_{时间戳}_{随机数}
     */
    private String generateTransactionNo(String payMethod) {
        return "PAY_" + payMethod.toUpperCase() + "_"
                + System.currentTimeMillis() + "_"
                + (1000 + (int) (Math.random() * 9000));
    }

    /**
     * 写入订单状态日志
     */
    private void writeStatusLog(Integer orderId, String fromStatus, String toStatus,
                                 Integer operatorId, String remark) {
        OrderStatusLog logEntry = new OrderStatusLog(orderId, fromStatus, toStatus, operatorId, remark);
        statusLogRepository.save(logEntry);
    }

    /**
     * 构建支付响应DTO
     */
    private PaymentResponse buildPaymentResponse(Payment payment, Order order) {
        PaymentResponse resp = new PaymentResponse();
        resp.setId(payment.getId());
        resp.setOrderId(payment.getOrderId());
        resp.setPayMethod(payment.getPayMethod());
        resp.setPayStatus(payment.getPayStatus());
        resp.setTransactionNo(payment.getTransactionNo());
        resp.setPaidAmount(payment.getPaidAmount());
        resp.setPaidAt(payment.getPaidAt());
        resp.setRefundedAt(payment.getRefundedAt());

        if (order != null) {
            resp.setOrderStatus(order.getOrderStatus());
            resp.setOrderCreatedAt(order.getCreatedAt());

            // 查询商家名称
            merchantDetailRepository.findById(order.getMerchantId())
                    .ifPresent(m -> resp.setMerchantName(m.getShopName()));
        }

        return resp;
    }

    /**
     * 手动内存分页（用于从List转Page）
     */
    private Page<PaymentResponse> manualPage(List<PaymentResponse> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());

        if (start > list.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, list.size());
        }

        return new PageImpl<>(list.subList(start, end), pageable, list.size());
    }
}
