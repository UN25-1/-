package com.example.demo.service;

import com.example.demo.dto.OrderResponse;
import com.example.demo.dto.RiderDetailRequest;
import com.example.demo.dto.RiderDetailResponse;
import com.example.demo.entity.*;
import com.example.demo.exception.BusinessException;
import com.example.demo.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 骑手服务 —— 骑手认证 + 在线状态管理 + 接单 + 配送 + 订单查询
 *
 * 所有数据库操作均通过 JPA 参数化查询执行，防止SQL注入
 */
@Service
public class RiderService {

    private static final Logger log = LoggerFactory.getLogger(RiderService.class);

    /** 允许切换的状态 */
    private static final Set<String> VALID_STATUSES = Set.of("offline", "online", "busy");

    /** 骑手可接单的订单状态：只有商家备餐完成后才能被骑手看到 */
    private static final Set<String> GRABBABLE_STATUSES = Set.of("prepared");

    /** 骑手可上报异常的订单状态：配送中 */
    private static final Set<String> EXCEPTION_REPORTABLE_STATUSES = Set.of("delivering");

    private final RiderDetailRepository riderDetailRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusLogRepository statusLogRepository;
    private final UserRepository userRepository;
    private final MerchantDetailRepository merchantDetailRepository;
    private final ProductRepository productRepository;

    public RiderService(RiderDetailRepository riderDetailRepository,
                        OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        OrderStatusLogRepository statusLogRepository,
                        UserRepository userRepository,
                        MerchantDetailRepository merchantDetailRepository,
                        ProductRepository productRepository) {
        this.riderDetailRepository = riderDetailRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.statusLogRepository = statusLogRepository;
        this.userRepository = userRepository;
        this.merchantDetailRepository = merchantDetailRepository;
        this.productRepository = productRepository;
    }

    // ==================== 骑手认证（档案管理） ====================

    /**
     * 获取骑手详情：若档案不存在则自动创建空档案
     */
    private RiderDetail getOrCreateRiderDetail(Integer userId) {
        return riderDetailRepository.findByUserId(userId)
                .orElseGet(() -> {
                    RiderDetail newDetail = new RiderDetail(userId);
                    return riderDetailRepository.save(newDetail);
                });
    }

    /**
     * 查看骑手档案
     */
    public RiderDetailResponse getProfile(Integer userId) {
        RiderDetail detail = getOrCreateRiderDetail(userId);
        User user = userRepository.findById(userId).orElse(null);
        return RiderDetailResponse.from(detail, user);
    }

    /**
     * 完善/更新骑手档案（认证信息：实名、身份证、车辆）
     */
    @Transactional
    public RiderDetailResponse updateProfile(Integer userId, RiderDetailRequest request) {
        RiderDetail detail = getOrCreateRiderDetail(userId);
        detail.setRealName(request.getRealName());
        detail.setIdCard(request.getIdCard());
        detail.setVehicle(request.getVehicle());
        detail.setVehicleNumber(request.getVehicleNumber());
        riderDetailRepository.save(detail);

        log.info("骑手档案已更新：userId={}, realName={}", userId, request.getRealName());
        return getProfile(userId);
    }

    // ==================== 在线状态管理 ====================

    /**
     * 切换骑手在线状态：offline / online / busy
     * - online：可接单
     * - busy：配送中（接单后自动切换）
     * - offline：不可接单
     */
    @Transactional
    public RiderDetailResponse updateStatus(Integer userId, String newStatus) {
        if (!VALID_STATUSES.contains(newStatus)) {
            throw new BusinessException(400, "无效的状态[" + newStatus + "]，仅支持 offline / online / busy");
        }

        RiderDetail detail = getOrCreateRiderDetail(userId);
        String oldStatus = detail.getStatus();
        detail.setStatus(newStatus);
        riderDetailRepository.save(detail);

        log.info("骑手状态变更：userId={}, {} → {}", userId, oldStatus, newStatus);
        return getProfile(userId);
    }

    // ==================== 可接订单列表 ====================

    /**
     * 骑手查看可接订单（状态为 prepared 且尚未分配骑手）
     */
    public List<OrderResponse> getAvailableOrders() {
        List<Order> orders = orderRepository.findByOrderStatusInAndRiderIdIsNullOrderByCreatedAtAsc(
                List.of("prepared"));

        return buildOrderResponseList(orders);
    }

    // ==================== 接单（抢单） ====================

    /**
     * 骑手抢单：将订单 rider_id 设为自己，状态保持不变
     * - 仅 online 状态可接单
     * - 订单状态必须为 prepared 且尚无骑手
     */
    @Transactional
    public OrderResponse grabOrder(Integer orderId, Integer userId) {
        // 1. 校验骑手在线状态
        RiderDetail rider = getOrCreateRiderDetail(userId);
        if (!"online".equals(rider.getStatus())) {
            throw new BusinessException(400, "当前状态为[" + rider.getStatus() + "]，请先切换为 online 才能接单");
        }

        // 2. 校验骑手档案完整性
        if (rider.getRealName() == null || rider.getIdCard() == null) {
            throw new BusinessException(400, "请先完善骑手认证信息（真实姓名、身份证号）");
        }

        // 3. 查询并校验订单
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));

        if (!GRABBABLE_STATUSES.contains(order.getOrderStatus())) {
            throw new BusinessException(400, "订单当前状态[" + order.getOrderStatus() + "]不允许接单");
        }

        if (order.getRiderId() != null) {
            throw new BusinessException(400, "该订单已被其他骑手接单");
        }

        // 4. 分配骑手
        order.setRiderId(rider.getId());
        orderRepository.save(order);

        // 5. 骑手状态切换为 busy
        rider.setStatus("busy");
        riderDetailRepository.save(rider);

        // 6. 写入状态日志
        writeStatusLog(orderId, order.getOrderStatus(), order.getOrderStatus(),
                userId, "骑手[" + userId + "]接单");

        log.info("骑手接单成功：orderId={}, riderId={}, userId={}", orderId, rider.getId(), userId);

        return buildOrderDetail(orderId);
    }

    // ==================== 取餐 ====================

    /**
     * 骑手到店取餐：preparing/prepared → delivering
     * 仅分配了该骑手的订单才能操作
     */
    @Transactional
    public OrderResponse pickupOrder(Integer orderId, Integer userId) {
        Integer riderId = resolveRiderIdFromUserId(userId);
        Order order = validateRiderOrderOwnership(orderId, riderId);

        if (!"preparing".equals(order.getOrderStatus()) && !"prepared".equals(order.getOrderStatus())) {
            throw new BusinessException(400, "当前订单状态[" + order.getOrderStatus() + "]，仅 preparing 或 prepared 状态可以取餐");
        }

        String oldStatus = order.getOrderStatus();
        order.setOrderStatus("delivering");
        orderRepository.save(order);

        writeStatusLog(orderId, oldStatus, "delivering", userId, "骑手取餐，开始配送");

        log.info("骑手取餐：orderId={}, riderId={}, status=delivering", orderId, riderId);
        return buildOrderDetail(orderId);
    }

    // ==================== 配送完成 ====================

    /**
     * 骑手送达：delivering → delivered，自动累加 completed_orders
     */
    @Transactional
    public OrderResponse deliverOrder(Integer orderId, Integer userId) {
        Integer riderId = resolveRiderIdFromUserId(userId);
        Order order = validateRiderOrderOwnership(orderId, riderId);

        if (!"delivering".equals(order.getOrderStatus())) {
            throw new BusinessException(400, "当前订单状态[" + order.getOrderStatus() + "]，仅 delivering 状态可以确认送达");
        }

        String oldStatus = order.getOrderStatus();
        order.setOrderStatus("delivered");
        orderRepository.save(order);

        writeStatusLog(orderId, oldStatus, "delivered", userId, "骑手已送达");

        // 自动累加完成单数
        RiderDetail rider = getOrCreateRiderDetail(userId);
        rider.setCompletedOrders(rider.getCompletedOrders() + 1);
        riderDetailRepository.save(rider);

        // 送达后自动恢复在线状态，允许继续接单
        if ("busy".equals(rider.getStatus())) {
            rider.setStatus("online");
            riderDetailRepository.save(rider);
        }

        log.info("骑手送达：orderId={}, riderId={}, completedOrders={}, status=delivered, riderStatus恢复为online",
                orderId, riderId, rider.getCompletedOrders());
        return buildOrderDetail(orderId);
    }

    // ==================== 配送异常上报 ====================

    /**
     * 骑手上报配送异常：delivering → exception
     * 用于骑手遇到无法送达的情况（如用户失联、地址错误等）
     */
    @Transactional
    public OrderResponse reportException(Integer orderId, Integer userId, String reason) {
        Integer riderId = resolveRiderIdFromUserId(userId);
        Order order = validateRiderOrderOwnership(orderId, riderId);

        if (!EXCEPTION_REPORTABLE_STATUSES.contains(order.getOrderStatus())) {
            throw new BusinessException(400, "当前订单状态[" + order.getOrderStatus()
                    + "]不允许上报异常，仅配送中的订单可上报");
        }

        String oldStatus = order.getOrderStatus();
        order.setOrderStatus("exception");
        orderRepository.save(order);

        writeStatusLog(orderId, oldStatus, "exception", userId,
                reason != null ? reason : "骑手上报配送异常");

        // 恢复骑手为在线状态，允许继续接单
        RiderDetail rider = getOrCreateRiderDetail(userId);
        if ("busy".equals(rider.getStatus())) {
            rider.setStatus("online");
            riderDetailRepository.save(rider);
        }

        log.warn("骑手上报配送异常：orderId={}, riderId={}, reason={}", orderId, riderId, reason);
        return buildOrderDetail(orderId);
    }

    /**
     * 骑手查询自己的配送订单（支持按状态筛选和分页）
     */
    public Page<OrderResponse> getMyOrders(Integer userId, String status, Pageable pageable) {
        Integer riderId = resolveRiderIdFromUserId(userId);
        Page<Order> orderPage;
        if (status != null && !status.isEmpty()) {
            orderPage = orderRepository.findByRiderIdAndOrderStatusOrderByCreatedAtDesc(riderId, status, pageable);
        } else {
            orderPage = orderRepository.findByRiderIdOrderByCreatedAtDesc(riderId, pageable);
        }
        return orderPage.map(order -> buildSimpleOrderResponse(order));
    }

    // ==================== 内部校验方法 ====================

    /**
     * 通过 userId 解析 riderId（查询 rider_details 表）
     */
    private Integer resolveRiderIdFromUserId(Integer userId) {
        return riderDetailRepository.findByUserId(userId)
                .map(RiderDetail::getId)
                .orElseThrow(() -> new BusinessException(404, "未找到骑手档案，请先完善信息"));
    }

    /**
     * 校验订单是否分配给指定骑手
     */
    private Order validateRiderOrderOwnership(Integer orderId, Integer riderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
        if (order.getRiderId() == null || !order.getRiderId().equals(riderId)) {
            throw new BusinessException(403, "该订单未分配给您，无法操作");
        }
        return order;
    }

    // ==================== 内部工具方法 ====================

    /**
     * 写入状态变更日志
     */
    private void writeStatusLog(Integer orderId, String fromStatus, String toStatus,
                                 Integer operatorId, String remark) {
        OrderStatusLog log = new OrderStatusLog(orderId, fromStatus, toStatus, operatorId, remark);
        statusLogRepository.save(log);
    }

    /**
     * 构建订单详情响应（含明细 + 状态日志 + 商家 + 骑手 + 用户信息）
     */
    private OrderResponse buildOrderDetail(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));

        MerchantDetail merchant = merchantDetailRepository.findById(order.getMerchantId()).orElse(null);
        User user = userRepository.findById(order.getUserId()).orElse(null);

        // 骑手信息
        User riderUser = null;
        if (order.getRiderId() != null) {
            RiderDetail riderDetail = riderDetailRepository.findById(order.getRiderId()).orElse(null);
            if (riderDetail != null) {
                riderUser = userRepository.findById(riderDetail.getUserId()).orElse(null);
            }
        }

        OrderResponse resp = buildSimpleOrderResponse(order);

        // 填充商家名称
        if (merchant != null) {
            resp.setMerchantName(merchant.getShopName());
        }

        // 填充用户名
        if (user != null) {
            resp.setUsername(user.getUsername());
        }

        // 填充骑手名
        if (riderUser != null) {
            resp.setRiderName(riderUser.getUsername());
        }

        // 查询订单明细
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        List<Integer> productIds = orderItems.stream()
                .map(OrderItem::getProductId)
                .collect(Collectors.toList());
        Map<Integer, Product> productMap = productRepository.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<com.example.demo.dto.OrderItemResponse> itemResponses = orderItems.stream()
                .map(item -> {
                    Product product = productMap.get(item.getProductId());
                    String productName = product != null ? product.getName() : "商品已下架";
                    String imageUrl = product != null ? product.getImageUrl() : null;
                    return com.example.demo.dto.OrderItemResponse.of(
                            item.getId(), item.getProductId(),
                            productName, imageUrl,
                            item.getPrice(), item.getQuantity()
                    );
                })
                .collect(Collectors.toList());
        resp.setItems(itemResponses);

        // 查询状态日志
        List<OrderStatusLog> statusLogs = statusLogRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
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
        resp.setStatusLogs(logEntries);

        return resp;
    }

    /**
     * 构建简化订单响应（不含明细）
     */
    private OrderResponse buildSimpleOrderResponse(Order order) {
        OrderResponse resp = new OrderResponse();
        resp.setId(order.getId());
        resp.setUserId(order.getUserId());
        resp.setMerchantId(order.getMerchantId());
        resp.setRiderId(order.getRiderId());
        resp.setOrderStatus(order.getOrderStatus());
        resp.setTotalAmount(order.getTotalAmount());
        resp.setDeliveryFee(order.getDeliveryFee());
        resp.setDeliveryAddress(order.getDeliveryAddress());
        resp.setContactPhone(order.getContactPhone());
        resp.setContactName(order.getContactName());
        resp.setNote(order.getNote());
        resp.setCreatedAt(order.getCreatedAt());
        resp.setUpdatedAt(order.getUpdatedAt());
        return resp;
    }

    /**
     * 批量构建订单响应列表
     */
    private List<OrderResponse> buildOrderResponseList(List<Order> orders) {
        // 批量查询商家信息
        Set<Integer> merchantIds = orders.stream()
                .map(Order::getMerchantId)
                .collect(Collectors.toSet());
        Map<Integer, MerchantDetail> merchantMap = merchantDetailRepository.findAllById(merchantIds)
                .stream()
                .collect(Collectors.toMap(MerchantDetail::getId, m -> m));

        return orders.stream().map(order -> {
            OrderResponse resp = buildSimpleOrderResponse(order);
            MerchantDetail merchant = merchantMap.get(order.getMerchantId());
            if (merchant != null) {
                resp.setMerchantName(merchant.getShopName());
            }
            return resp;
        }).collect(Collectors.toList());
    }
}
