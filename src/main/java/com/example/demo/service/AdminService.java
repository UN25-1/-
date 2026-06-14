package com.example.demo.service;

import com.example.demo.dto.AdminStatsResponse;
import com.example.demo.dto.OrderResponse;
import com.example.demo.dto.UserProfileResponse;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理员服务 —— 用户管理 + 商家审核 + 订单监控 + 骑手审核 + 数据统计
 * 所有数据库操作均通过 JPA 参数化查询执行，防止SQL注入
 */
@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusLogRepository statusLogRepository;
    private final MerchantDetailRepository merchantDetailRepository;
    private final RiderDetailRepository riderDetailRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;

    private final AuditLogRepository auditLogRepository;

    public AdminService(UserRepository userRepository,
                        OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        OrderStatusLogRepository statusLogRepository,
                        MerchantDetailRepository merchantDetailRepository,
                        RiderDetailRepository riderDetailRepository,
                        ProductRepository productRepository,
                        PaymentRepository paymentRepository,
                        AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.statusLogRepository = statusLogRepository;
        this.merchantDetailRepository = merchantDetailRepository;
        this.riderDetailRepository = riderDetailRepository;
        this.productRepository = productRepository;
        this.paymentRepository = paymentRepository;
        this.auditLogRepository = auditLogRepository;
    }

    // ==================== 用户管理 ====================

    /**
     * 管理员查看所有用户（分页，支持关键词搜索和状态筛选）
     * @param status 用户状态筛选：null=全部, 0=禁用, 1=启用, 2=待审核
     */
    public Page<UserProfileResponse> getAllUsers(String keyword, Integer status, Pageable pageable) {
        Page<User> userPage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            userPage = userRepository.findByUsernameContaining(keyword.trim(), pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        // 按状态筛选
        if (status != null) {
            List<User> filtered = userPage.getContent().stream()
                    .filter(u -> u.getStatus() != null && u.getStatus().equals(status))
                    .collect(Collectors.toList());
            userPage = new PageImpl<>(filtered, pageable, userPage.getTotalElements());
        }

        return userPage.map(this::buildUserProfileResponse);
    }

    /**
     * 更新用户状态
     * @param userId 用户ID
     * @param status 目标状态：0=禁用, 1=启用, 2=待审核
     */
    @Transactional
    public void updateUserStatus(Integer userId, int status) {
        if (status < 0 || status > 2) {
            throw new BusinessException(400, "无效的状态值[" + status + "]，仅支持 0(禁用)、1(启用)、2(待审核)");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        Integer oldStatus = user.getStatus();
        user.setStatus(status);
        userRepository.save(user);

        String statusName = status == 0 ? "禁用" : status == 1 ? "启用" : "设为待审核";
        log.info("管理员操作：{} 用户 userId={}, username={}, status: {} → {}",
                statusName, userId, user.getUsername(), oldStatus, status);

        writeAuditLog("USER", userId, "UPDATE_STATUS",
                "status: " + oldStatus + " → " + status,
                user.getUsername(), null);
    }

    /**
     * 查看用户详情
     */
    public UserProfileResponse getUserDetail(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        return buildUserProfileResponse(user);
    }

    // ==================== 商家审核 ====================

    /**
     * 商家审核列表（支持按店铺名称搜索 + enabled 筛选）
     * @param enabled null=全部, true=已启用, false=已禁用
     */
    public Page<com.example.demo.dto.MerchantDetailResponse> getMerchantList(
            String keyword, Boolean enabled, Pageable pageable) {
        List<MerchantDetail> allMerchants;
        if (keyword != null && !keyword.trim().isEmpty()) {
            allMerchants = merchantDetailRepository.findByShopNameContaining(keyword.trim());
        } else {
            allMerchants = merchantDetailRepository.findAll();
        }

        // 按 enabled 筛选
        if (enabled != null) {
            allMerchants = allMerchants.stream()
                    .filter(m -> enabled.equals(m.getEnabled()))
                    .collect(Collectors.toList());
        }

        // 手动分页
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allMerchants.size());
        if (start > allMerchants.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, allMerchants.size());
        }
        List<com.example.demo.dto.MerchantDetailResponse> content = allMerchants.subList(start, end)
                .stream().map(com.example.demo.dto.MerchantDetailResponse::from)
                .collect(Collectors.toList());
        return new PageImpl<>(content, pageable, allMerchants.size());
    }

    /**
     * 审核商家（启用/禁用）
     */
    @Transactional
    public void updateMerchantStatus(Integer merchantId, boolean enabled) {
        MerchantDetail merchant = merchantDetailRepository.findById(merchantId)
                .orElseThrow(() -> new BusinessException(404, "商家不存在"));

        boolean oldEnabled = Boolean.TRUE.equals(merchant.getEnabled());
        merchant.setEnabled(enabled);
        merchantDetailRepository.save(merchant);

        log.info("管理员操作：{} 商家 merchantId={}, shopName={}",
                enabled ? "启用" : "禁用", merchantId, merchant.getShopName());

        writeAuditLog("MERCHANT", merchantId, "UPDATE_STATUS",
                "enabled: " + oldEnabled + " → " + enabled,
                merchant.getShopName(), null);
    }

    // ==================== 全平台订单监控 ====================

    /**
     * 全平台订单查询（支持按状态筛选 + 分页）
     */
    public Page<OrderResponse> getAllOrders(String status, Pageable pageable) {
        Page<Order> orderPage;
        if (status != null && !status.isEmpty()) {
            orderPage = orderRepository.findByOrderStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            orderPage = orderRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        // 批量加载商家信息
        Set<Integer> merchantIds = orderPage.getContent().stream()
                .map(Order::getMerchantId).collect(Collectors.toSet());
        Map<Integer, MerchantDetail> merchantMap = merchantDetailRepository.findAllById(merchantIds)
                .stream().collect(Collectors.toMap(MerchantDetail::getId, m -> m));

        // 批量加载用户信息
        Set<Integer> userIds = orderPage.getContent().stream()
                .map(Order::getUserId).collect(Collectors.toSet());
        Map<Integer, User> userMap = userRepository.findAllById(userIds)
                .stream().collect(Collectors.toMap(User::getId, u -> u));

        return orderPage.map(order -> {
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

            User user = userMap.get(order.getUserId());
            if (user != null) resp.setUsername(user.getUsername());

            MerchantDetail merchant = merchantMap.get(order.getMerchantId());
            if (merchant != null) resp.setMerchantName(merchant.getShopName());

            return resp;
        });
    }

    // ==================== 骑手审核 ====================

    /**
     * 骑手列表（支持按姓名/身份证搜索 + enabled 筛选）
     * @param enabled null=全部, true=已启用, false=已禁用
     */
    public Page<com.example.demo.dto.RiderDetailResponse> getRiderList(String keyword, Boolean enabled, Pageable pageable) {
        List<RiderDetail> allRiders = riderDetailRepository.findAll();

        // 按关键词过滤
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            Set<Integer> matchingUserIds = userRepository.findByUsernameContaining(kw, Pageable.unpaged())
                    .stream().map(User::getId).collect(Collectors.toSet());
            allRiders = allRiders.stream()
                    .filter(r -> (r.getRealName() != null && r.getRealName().contains(kw))
                            || (r.getIdCard() != null && r.getIdCard().contains(kw))
                            || matchingUserIds.contains(r.getUserId()))
                    .collect(Collectors.toList());
        }

        // 按 enabled 筛选
        if (enabled != null) {
            allRiders = allRiders.stream()
                    .filter(r -> enabled.equals(r.getEnabled()))
                    .collect(Collectors.toList());
        }

        // 手动分页
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allRiders.size());
        if (start > allRiders.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, allRiders.size());
        }
        Map<Integer, User> userMap = userRepository.findAllById(
                allRiders.stream().map(RiderDetail::getUserId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(User::getId, u -> u));

        List<com.example.demo.dto.RiderDetailResponse> content = allRiders.subList(start, end)
                .stream().map(r -> {
                    User user = userMap.get(r.getUserId());
                    return com.example.demo.dto.RiderDetailResponse.from(r, user);
                }).collect(Collectors.toList());
        return new PageImpl<>(content, pageable, allRiders.size());
    }

    /**
     * 审核骑手（启用/禁用）
     */
    @Transactional
    public void updateRiderStatus(Integer riderId, boolean enabled) {
        RiderDetail rider = riderDetailRepository.findById(riderId)
                .orElseThrow(() -> new BusinessException(404, "骑手不存在"));

        boolean oldEnabled = Boolean.TRUE.equals(rider.getEnabled());
        rider.setEnabled(enabled);
        riderDetailRepository.save(rider);

        log.info("管理员操作：{} 骑手 riderId={}, realName={}",
                enabled ? "启用" : "禁用", riderId, rider.getRealName());

        writeAuditLog("RIDER", riderId, "UPDATE_STATUS",
                "enabled: " + oldEnabled + " → " + enabled,
                rider.getRealName(), null);
    }

    // ==================== 数据统计看板 ====================

    /**
     * 获取管理员统计看板数据（使用数据库层聚合查询，避免全表加载）
     */
    public AdminStatsResponse getDashboardStats() {
        AdminStatsResponse stats = new AdminStatsResponse();

        // 总数统计
        stats.setTotalUsers(userRepository.count());
        stats.setTotalMerchants(merchantDetailRepository.count());
        stats.setTotalRiders(riderDetailRepository.count());

        // 今日数据
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);

        List<Order> todayOrders = orderRepository.findByCreatedAtBetween(todayStart, todayEnd);
        stats.setTodayOrderCount(todayOrders.size());

        BigDecimal todayGMV = todayOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTodayGMV(todayGMV);

        // 待处理订单数（pending + preparing + prepared + delivering）
        long pendingCount = orderRepository.countByOrderStatusIn(
                List.of("pending", "preparing", "prepared", "delivering"));
        stats.setPendingOrderCount(pendingCount);

        // 各状态订单分布（使用数据库 GROUP BY 聚合，不再全表加载到内存）
        List<Object[]> statusCounts = orderRepository.countGroupByOrderStatus();
        Map<String, Long> distribution = new java.util.LinkedHashMap<>();
        for (Object[] row : statusCounts) {
            distribution.put((String) row[0], (Long) row[1]);
        }
        stats.setOrderStatusDistribution(distribution);

        // 热门商家 Top 5（使用数据库聚合查询）
        List<Object[]> merchantOrderCounts = orderRepository.countByMerchantIdGrouped();
        Map<Integer, Long> merchantOrderCountMap = new java.util.HashMap<>();
        for (Object[] row : merchantOrderCounts) {
            merchantOrderCountMap.put((Integer) row[0], (Long) row[1]);
        }

        List<Object[]> merchantRevenues = orderRepository.sumAmountByMerchantIdGrouped();
        Map<Integer, BigDecimal> merchantRevenueMap = new java.util.HashMap<>();
        for (Object[] row : merchantRevenues) {
            merchantRevenueMap.put((Integer) row[0], (BigDecimal) row[1]);
        }

        Set<Integer> topMerchantIds = merchantOrderCountMap.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        Map<Integer, MerchantDetail> merchants = merchantDetailRepository.findAllById(topMerchantIds)
                .stream().collect(Collectors.toMap(MerchantDetail::getId, m -> m));

        List<AdminStatsResponse.TopMerchant> topMerchants = topMerchantIds.stream()
                .map(id -> AdminStatsResponse.TopMerchant.of(
                        id,
                        merchants.containsKey(id) ? merchants.get(id).getShopName() : "未知商家",
                        merchantOrderCountMap.getOrDefault(id, 0L),
                        merchantRevenueMap.getOrDefault(id, BigDecimal.ZERO)))
                .sorted((a, b) -> Long.compare(b.getOrderCount(), a.getOrderCount()))
                .collect(Collectors.toList());
        stats.setTopMerchants(topMerchants);

        return stats;
    }

    // ==================== 内部工具方法 ====================

    /**
     * 构建用户信息响应
     */
    private UserProfileResponse buildUserProfileResponse(User user) {
        UserProfileResponse resp = new UserProfileResponse();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setPhone(user.getPhone());
        resp.setRole(user.getRole());
        resp.setEnabled(user.getStatus() != null && user.getStatus() == 1);
        resp.setStatus(user.getStatus());
        resp.setCreatedAt(user.getCreatedAt());
        return resp;
    }

    /**
     * 写入审计日志（异步记录异常不影响主流程）
     */
    private void writeAuditLog(String targetType, Integer targetId, String action,
                                String detail, String targetName, Integer operatorId) {
        try {
            AuditLog auditLog = new AuditLog(targetType, targetId, action, detail, targetName, operatorId);
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("写入审计日志失败: targetType={}, targetId={}, action={}", targetType, targetId, action, e);
        }
    }
}
