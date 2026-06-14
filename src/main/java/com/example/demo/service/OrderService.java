package com.example.demo.service;

import com.example.demo.dto.CartItemResponse;
import com.example.demo.dto.OrderItemResponse;
import com.example.demo.dto.OrderRequest;
import com.example.demo.dto.OrderResponse;
import com.example.demo.entity.*;
import com.example.demo.exception.BusinessException;
import com.example.demo.repository.*;
import com.example.demo.entity.RiderDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单服务 —— 下单 + 状态流转 + 多角色查询 + 支付与退款联动
 * 所有数据库操作均通过 JPA 参数化查询执行，防止SQL注入
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    /** 允许取消的状态：待支付、待处理 */
    private static final Set<String> CANCELLABLE_STATUSES = Set.of("pending_payment", "pending");

    /** 商家可拒绝的订单状态 */
    private static final Set<String> REJECTABLE_STATUSES = Set.of("pending");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusLogRepository statusLogRepository;
    private final UserRepository userRepository;
    private final UserAddressRepository addressRepository;
    private final MerchantDetailRepository merchantDetailRepository;
    private final ProductRepository productRepository;
    private final RiderDetailRepository riderDetailRepository;
    private final CartService cartService;
    private final PaymentService paymentService;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        OrderStatusLogRepository statusLogRepository,
                        UserRepository userRepository,
                        UserAddressRepository addressRepository,
                        MerchantDetailRepository merchantDetailRepository,
                        ProductRepository productRepository,
                        RiderDetailRepository riderDetailRepository,
                        CartService cartService,
                        PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.statusLogRepository = statusLogRepository;
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.merchantDetailRepository = merchantDetailRepository;
        this.productRepository = productRepository;
        this.riderDetailRepository = riderDetailRepository;
        this.cartService = cartService;
        this.paymentService = paymentService;
    }

    // ==================== 下单 ====================

    /**
     * 用户下单：从购物车创建订单（自动按商家拆分）
     * 流程：校验地址 → 拆分购物车 → 逐商家校验起送价 → 创建订单+明细 → 写状态日志 → 清空购物车
     *
     * @param userId  下单用户ID
     * @param request 包含地址ID和备注
     * @return 创建的订单列表（跨商家时返回多个订单）
     */
    @Transactional
    public List<OrderResponse> createOrder(Integer userId, OrderRequest request) {
        // 1. 校验配送地址
        UserAddress address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new BusinessException(404, "地址不存在"));
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权使用该地址");
        }

        // 2. 拆分购物车（按商家分组）
        Map<Integer, List<CartItemResponse>> merchantGroups = cartService.splitByMerchant(userId);
        if (merchantGroups.isEmpty()) {
            throw new BusinessException(400, "购物车为空，请先添加商品");
        }

        // 3. 批量查询商家信息
        Set<Integer> merchantIds = merchantGroups.keySet();
        Map<Integer, MerchantDetail> merchantMap = merchantDetailRepository.findAllById(merchantIds)
                .stream()
                .collect(Collectors.toMap(MerchantDetail::getId, m -> m));

        List<OrderResponse> createdOrders = new ArrayList<>();

        // 4. 逐商家创建订单
        for (Map.Entry<Integer, List<CartItemResponse>> entry : merchantGroups.entrySet()) {
            Integer merchantId = entry.getKey();
            List<CartItemResponse> items = entry.getValue();

            MerchantDetail merchant = merchantMap.get(merchantId);
            if (merchant == null) {
                throw new BusinessException(404, "商家[" + merchantId + "]不存在");
            }

            // 计算商品小计
            BigDecimal subtotal = items.stream()
                    .map(CartItemResponse::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 校验起送价
            BigDecimal minOrder = merchant.getMinOrderAmount() != null
                    ? merchant.getMinOrderAmount() : BigDecimal.ZERO;
            if (subtotal.compareTo(minOrder) < 0) {
                throw new BusinessException(400, "商家[" + merchant.getShopName()
                        + "]未达到起送价 ¥" + minOrder + "，当前小计 ¥" + subtotal);
            }

            // 校验营业时间
            if (merchant.getOpeningTime() != null && merchant.getClosingTime() != null) {
                LocalTime now = LocalTime.now();
                if (now.isBefore(merchant.getOpeningTime()) || now.isAfter(merchant.getClosingTime())) {
                    throw new BusinessException(400, "商家[" + merchant.getShopName()
                            + "]当前不在营业时间内（" + merchant.getOpeningTime() + " - " + merchant.getClosingTime() + "）");
                }
            }

            // 计算总金额 = 商品小计 + 配送费
            BigDecimal deliveryFee = merchant.getDeliveryFee() != null
                    ? merchant.getDeliveryFee() : BigDecimal.ZERO;
            BigDecimal totalAmount = subtotal.add(deliveryFee);

            // 创建订单
            Order order = new Order();
            order.setUserId(userId);
            order.setMerchantId(merchantId);
            order.setOrderStatus("pending_payment");
            order.setTotalAmount(totalAmount);
            order.setDeliveryFee(deliveryFee);
            order.setDeliveryAddress(address.getAddress());
            order.setContactPhone(address.getPhone());
            order.setContactName(address.getContactName());
            order.setNote(request.getNote());
            order = orderRepository.save(order);

            // 创建订单明细 + 校验库存并扣减
            List<OrderItem> orderItems = new ArrayList<>();
            for (CartItemResponse item : items) {
                Product product = productRepository.findById(item.getProductId())
                        .orElseThrow(() -> new BusinessException(404, "商品[" + item.getProductId() + "]不存在"));

                // 校验库存
                if (product.getStock() < item.getQuantity()) {
                    throw new BusinessException(400, "商品[" + product.getName()
                            + "]库存不足（剩余" + product.getStock() + "，需要" + item.getQuantity() + "）");
                }

                // 扣减库存
                product.setStock(product.getStock() - item.getQuantity());
                productRepository.save(product);

                OrderItem orderItem = new OrderItem(
                        order.getId(),
                        item.getProductId(),
                        item.getQuantity(),
                        item.getProductPrice()
                );
                orderItems.add(orderItem);
            }
            orderItemRepository.saveAll(orderItems);

            // 写入状态日志
            writeStatusLog(order.getId(), null, "pending_payment", userId, null);

            // 清空对应的购物车项
            List<Integer> clearedProductIds = items.stream()
                    .map(CartItemResponse::getProductId)
                    .collect(Collectors.toList());
            cartService.clearCartAfterOrder(userId, clearedProductIds);

            log.info("订单创建成功：orderId={}, userId={}, merchantId={}, totalAmount={}, status=pending_payment",
                    order.getId(), userId, merchantId, totalAmount);

            // 构建响应（不包含明细以减少响应体积，明细需单独查询）
            OrderResponse resp = buildOrderResponse(order, merchant, null, null);
            createdOrders.add(resp);
        }

        return createdOrders;
    }

    // ==================== 支付（委托 PaymentService） ====================

    /**
     * 快捷支付：一键创建支付记录 + 模拟回调
     * 保持旧接口 PUT /api/orders/{id}/pay 可用，内部委托给 PaymentService
     *
     * @param orderId 订单ID
     * @param userId  当前用户ID
     * @return 支付后的订单详情
     */
    @Transactional
    public OrderResponse payOrder(Integer orderId, Integer userId) {
        paymentService.quickPay(orderId, userId);
        log.info("快捷支付完成：orderId={}, userId={}", orderId, userId);
        return getOrderDetail(orderId);
    }

    // ==================== 取消订单（含自动退款） ====================

    /**
     * 用户取消订单（仅 pending_payment 或 pending 状态可取消）
     * 
     * 取消时自动处理退款：
     * - pending 状态订单（已支付）：自动触发退款流程
     * - pending_payment 状态订单（未支付）：直接取消，无需退款
     */
    @Transactional
    public OrderResponse cancelOrder(Integer orderId, Integer userId) {
        Order order = validateOrderOwnership(orderId, userId);

        if (!CANCELLABLE_STATUSES.contains(order.getOrderStatus())) {
            throw new BusinessException(400, "当前订单状态[" + order.getOrderStatus() + "]不允许取消");
        }

        String oldStatus = order.getOrderStatus();

        // 如果订单已支付（status=pending），需要退款
        if ("pending".equals(oldStatus)) {
            paymentService.processRefund(orderId, userId, "用户主动取消订单");
        }

        // 更新订单状态
        order.setOrderStatus("cancelled");
        orderRepository.save(order);

        writeStatusLog(orderId, oldStatus, "cancelled", userId, null);

        // 恢复库存：将订单中的商品数量加回库存
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : orderItems) {
            productRepository.findById(item.getProductId()).ifPresent(product -> {
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
            });
        }

        log.info("订单已取消（含退款联动+库存恢复）：orderId={}, userId={}", orderId, userId);
        return getOrderDetail(orderId);
    }

    // ==================== 商家拒单（含自动退款+库存恢复） ====================

    /**
     * 商家拒绝接单：pending → rejected
     * - 自动退款
     * - 恢复库存
     */
    @Transactional
    public OrderResponse rejectOrder(Integer orderId, Integer userId, String reason) {
        Integer merchantId = resolveMerchantIdFromUserId(userId);
        Order order = validateMerchantOrderOwnership(orderId, merchantId);

        if (!REJECTABLE_STATUSES.contains(order.getOrderStatus())) {
            throw new BusinessException(400, "当前订单状态[" + order.getOrderStatus() + "]不允许拒单，仅待处理订单可拒单");
        }

        String oldStatus = order.getOrderStatus();

        // 已支付订单需退款
        if ("pending".equals(oldStatus)) {
            paymentService.processRefund(orderId, userId,
                    reason != null ? reason : "商家拒单：" + reason);
        }

        // 更新订单状态
        order.setOrderStatus("rejected");
        orderRepository.save(order);

        writeStatusLog(orderId, oldStatus, "rejected", userId,
                reason != null ? reason : "商家已拒单");

        // 恢复库存
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : orderItems) {
            productRepository.findById(item.getProductId()).ifPresent(product -> {
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
            });
        }

        log.info("商家拒单（含退款联动+库存恢复）：orderId={}, merchantId={}, reason={}",
                orderId, merchantId, reason);
        return getOrderDetail(orderId);
    }

    // ==================== 用户拒收 ====================

    /**
     * 用户拒收已送达订单：delivered → rejected（不自动退款，需人工处理）
     */
    @Transactional
    public OrderResponse rejectDelivery(Integer orderId, Integer userId) {
        Order order = validateOrderOwnership(orderId, userId);

        if (!"delivered".equals(order.getOrderStatus())) {
            throw new BusinessException(400, "当前订单状态[" + order.getOrderStatus() + "]不允许拒收，仅已送达订单可拒收");
        }

        String oldStatus = order.getOrderStatus();
        order.setOrderStatus("rejected");
        orderRepository.save(order);

        writeStatusLog(orderId, oldStatus, "rejected", userId, "用户拒收");

        log.info("用户拒收：orderId={}, userId={}", orderId, userId);
        return getOrderDetail(orderId);
    }

    /**
     * 商家接单：pending → preparing
     * @param userId 当前登录的商家用户ID，内部解析为 merchantId
     */
    @Transactional
    public OrderResponse acceptOrder(Integer orderId, Integer userId) {
        Integer merchantId = resolveMerchantIdFromUserId(userId);
        Order order = validateMerchantOrderOwnership(orderId, merchantId);

        if (!"pending".equals(order.getOrderStatus())) {
            throw new BusinessException(400, "当前订单状态[" + order.getOrderStatus() + "]不允许接单");
        }

        String oldStatus = order.getOrderStatus();
        order.setOrderStatus("preparing");
        orderRepository.save(order);

        writeStatusLog(orderId, oldStatus, "preparing", userId, null);

        log.info("商家接单：orderId={}, merchantId={}, status=preparing", orderId, merchantId);
        return getOrderDetail(orderId);
    }

    /**
     * 商家备餐完成：preparing → prepared
     * @param userId 当前登录的商家用户ID，内部解析为 merchantId
     */
    @Transactional
    public OrderResponse completePreparation(Integer orderId, Integer userId) {
        Integer merchantId = resolveMerchantIdFromUserId(userId);
        Order order = validateMerchantOrderOwnership(orderId, merchantId);

        if (!"preparing".equals(order.getOrderStatus())) {
            throw new BusinessException(400, "当前订单状态[" + order.getOrderStatus() + "]，仅 preparing 状态可标记备餐完成");
        }

        String oldStatus = order.getOrderStatus();
        order.setOrderStatus("prepared");
        orderRepository.save(order);

        writeStatusLog(orderId, oldStatus, "prepared", userId, "商家备餐完成，等待骑手取餐");

        log.info("商家备餐完成：orderId={}, merchantId={}, status=prepared", orderId, merchantId);
        return getOrderDetail(orderId);
    }

    // ==================== 确认收货 ====================

    /**
     * 用户确认收货：delivered → completed
     */
    @Transactional
    public OrderResponse completeOrder(Integer orderId, Integer userId) {
        Order order = validateOrderOwnership(orderId, userId);

        if (!"delivered".equals(order.getOrderStatus())) {
            throw new BusinessException(400, "当前订单状态[" + order.getOrderStatus() + "]不允许确认收货，仅已送达订单可确认");
        }

        String oldStatus = order.getOrderStatus();
        order.setOrderStatus("completed");
        orderRepository.save(order);

        writeStatusLog(orderId, oldStatus, "completed", userId, null);

        log.info("订单确认收货：orderId={}, userId={}, status=completed", orderId, userId);
        return getOrderDetail(orderId);
    }

    // ==================== 查询：用户视角 ====================

    /**
     * 用户查询自己的订单（支持按状态筛选和分页）
     */
    public Page<OrderResponse> getMyOrders(Integer userId, String status, Pageable pageable) {
        Page<Order> orderPage;
        if (status != null && !status.isEmpty()) {
            orderPage = orderRepository.findByUserIdAndOrderStatusOrderByCreatedAtDesc(userId, status, pageable);
        } else {
            orderPage = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        return orderPage.map(order -> buildOrderResponse(order, null, null, null));
    }

    // ==================== 查询：商家视角 ====================

    /**
     * 商家查询本店订单（支持按状态筛选和分页）
     * @param userId 当前登录的商家用户ID，内部解析为 merchantId
     */
    public Page<OrderResponse> getMerchantOrders(Integer userId, String status, Pageable pageable) {
        Integer merchantId = resolveMerchantIdFromUserId(userId);
        Page<Order> orderPage;
        if (status != null && !status.isEmpty()) {
            orderPage = orderRepository.findByMerchantIdAndOrderStatusOrderByCreatedAtDesc(merchantId, status, pageable);
        } else {
            orderPage = orderRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId, pageable);
        }

        return orderPage.map(order -> buildOrderResponse(order, null, null, null));
    }

    /**
     * 商家查看本店订单详情（含明细和状态日志，校验归属权）
     * @param userId 当前登录的商家用户ID，内部解析为 merchantId
     */
    public OrderResponse getMerchantOrderDetail(Integer orderId, Integer userId) {
        Integer merchantId = resolveMerchantIdFromUserId(userId);
        validateMerchantOrderOwnership(orderId, merchantId);
        return getOrderDetail(orderId);
    }

    // ==================== 订单详情 ====================

    /**
     * 用户查看自己的订单详情（含所有权校验）
     */
    public OrderResponse getUserOrderDetail(Integer orderId, Integer userId) {
        validateOrderOwnership(orderId, userId);
        return getOrderDetail(orderId);
    }

    /**
     * 获取订单详情（含明细和状态日志）
     * 内部方法，调用方需自行校验权限
     */
    public OrderResponse getOrderDetail(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));

        // 查询关联信息
        MerchantDetail merchant = merchantDetailRepository.findById(order.getMerchantId()).orElse(null);
        User user = userRepository.findById(order.getUserId()).orElse(null);
        // 骑手信息：若订单已分配骑手，查询骑手用户名
        User riderUser = null;
        if (order.getRiderId() != null) {
            RiderDetail riderDetail = riderDetailRepository.findById(order.getRiderId()).orElse(null);
            if (riderDetail != null) {
                riderUser = userRepository.findById(riderDetail.getUserId()).orElse(null);
            }
        }

        // 查询订单明细
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        List<Integer> productIds = orderItems.stream()
                .map(OrderItem::getProductId)
                .collect(Collectors.toList());
        Map<Integer, Product> productMap = productRepository.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<OrderItemResponse> itemResponses = orderItems.stream()
                .map(item -> {
                    Product product = productMap.get(item.getProductId());
                    String productName = product != null ? product.getName() : "商品已下架";
                    String imageUrl = product != null ? product.getImageUrl() : null;
                    return OrderItemResponse.of(
                            item.getId(), item.getProductId(),
                            productName, imageUrl,
                            item.getPrice(), item.getQuantity()
                    );
                })
                .collect(Collectors.toList());

        // 查询状态日志
        List<OrderStatusLog> statusLogs = statusLogRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
        // 批量查操作人名称
        Set<Integer> operatorIds = statusLogs.stream()
                .map(OrderStatusLog::getOperatorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, String> operatorNameMap = operatorIds.isEmpty()
                ? Collections.emptyMap()
                : userRepository.findAllById(operatorIds).stream()
                    .collect(Collectors.toMap(User::getId, User::getUsername));

        List<OrderResponse.StatusLogEntry> logEntries = statusLogs.stream()
                .map(log -> OrderResponse.StatusLogEntry.of(
                        log.getId(), log.getFromStatus(), log.getToStatus(),
                        log.getOperatorId(),
                        log.getOperatorId() != null
                                ? operatorNameMap.getOrDefault(log.getOperatorId(), "未知用户") : "系统",
                        log.getRemark(), log.getCreatedAt()
                ))
                .collect(Collectors.toList());

        // 构建完整响应
        OrderResponse resp = buildOrderResponse(order, merchant, user, riderUser);
        resp.setItems(itemResponses);
        resp.setStatusLogs(logEntries);
        return resp;
    }

    // ==================== 内部校验方法 ====================

    /**
     * 校验订单归属（用户视角），返回订单实体
     */
    private Order validateOrderOwnership(Integer orderId, Integer userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该订单");
        }
        return order;
    }

    /**
     * 校验订单归属（商家视角），返回订单实体
     */
    private Order validateMerchantOrderOwnership(Integer orderId, Integer merchantId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
        if (!order.getMerchantId().equals(merchantId)) {
            throw new BusinessException(403, "无权操作该订单，该订单不属于本店");
        }
        return order;
    }

    // ==================== 内部工具方法 ====================

    /**
     * 通过 userId 解析 merchantId（查询 merchant_details 表）
     */
    private Integer resolveMerchantIdFromUserId(Integer userId) {
        MerchantDetail merchant = merchantDetailRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "未找到商家信息，请先完善店铺资料"));
        return merchant.getId();
    }

    /**
     * 写入状态变更日志
     */
    private void writeStatusLog(Integer orderId, String fromStatus, String toStatus,
                                 Integer operatorId, String remark) {
        OrderStatusLog log = new OrderStatusLog(orderId, fromStatus, toStatus, operatorId, remark);
        statusLogRepository.save(log);
    }

    /**
     * 构建订单响应DTO（不含明细，明细按需填充）
     */
    private OrderResponse buildOrderResponse(Order order, MerchantDetail merchant,
                                              User user, User riderUser) {
        OrderResponse resp = new OrderResponse();
        resp.setId(order.getId());
        resp.setUserId(order.getUserId());
        resp.setUsername(user != null ? user.getUsername() : null);
        resp.setMerchantId(order.getMerchantId());
        if (merchant != null) {
            resp.setMerchantName(merchant.getShopName());
        }
        resp.setRiderId(order.getRiderId());
        if (riderUser != null) {
            resp.setRiderName(riderUser.getUsername());
        }
        resp.setOrderStatus(order.getOrderStatus());
        // subtotal = totalAmount - deliveryFee，避免前端重复叠加配送费
        BigDecimal subtotal = order.getDeliveryFee() != null
                ? order.getTotalAmount().subtract(order.getDeliveryFee())
                : order.getTotalAmount();
        resp.setSubtotal(subtotal);
        resp.setDeliveryFee(order.getDeliveryFee());
        resp.setTotalAmount(order.getTotalAmount());
        resp.setDeliveryAddress(order.getDeliveryAddress());
        resp.setContactPhone(order.getContactPhone());
        resp.setContactName(order.getContactName());
        resp.setNote(order.getNote());
        resp.setCreatedAt(order.getCreatedAt());
        resp.setUpdatedAt(order.getUpdatedAt());
        return resp;
    }
}
