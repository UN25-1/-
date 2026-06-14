package com.example.demo.service;

import com.example.demo.dto.MerchantReviewResponse;
import com.example.demo.dto.RiderReviewResponse;
import com.example.demo.entity.*;
import com.example.demo.exception.BusinessException;
import com.example.demo.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * 评价服务 —— 商家评价 + 骑手评价 + 评分自动聚合
 * 核心功能：
 * - 用户对已完成(delivered/completed)订单的商家和骑手分别评价
 * - 每笔订单对同一商家/骑手只能评价一次（数据库 UNIQUE 约束兜底）
 * - 评价提交后自动重新计算并更新 merchant_details.rating / rider_details.rating
 * - 支持按商家/骑手/用户维度查询评价列表（分页）
 */
@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    /** 允许评价的订单状态 */
    private static final List<String> REVIEWABLE_STATUSES = List.of("delivered", "completed");

    private final MerchantReviewRepository merchantReviewRepository;
    private final RiderReviewRepository riderReviewRepository;
    private final OrderRepository orderRepository;
    private final MerchantDetailRepository merchantDetailRepository;
    private final RiderDetailRepository riderDetailRepository;
    private final UserRepository userRepository;

    public ReviewService(MerchantReviewRepository merchantReviewRepository,
                         RiderReviewRepository riderReviewRepository,
                         OrderRepository orderRepository,
                         MerchantDetailRepository merchantDetailRepository,
                         RiderDetailRepository riderDetailRepository,
                         UserRepository userRepository) {
        this.merchantReviewRepository = merchantReviewRepository;
        this.riderReviewRepository = riderReviewRepository;
        this.orderRepository = orderRepository;
        this.merchantDetailRepository = merchantDetailRepository;
        this.riderDetailRepository = riderDetailRepository;
        this.userRepository = userRepository;
    }

    // ==================== 商家评价 ====================

    /**
     * 提交商家评价
     * 
     * 业务规则：
     * - 仅订单归属用户可评价
     * - 订单状态必须为 delivered 或 completed
     * - 同一用户对同一订单的商家只能评价一次
     * - 评价后自动更新商家平均评分
     *
     * @param userId  当前登录用户ID
     * @param orderId 订单ID
     * @param rating  评分（1-5）
     * @param comment 评论文本
     * @return 评价响应
     */
    @Transactional
    public MerchantReviewResponse submitMerchantReview(Integer userId, Integer orderId,
                                                        Integer rating, String comment) {
        // 0. 评分范围校验（1-5分）
        if (rating == null || rating < 1 || rating > 5) {
            throw new BusinessException(400, "评分必须在1-5分之间");
        }

        // 1. 校验订单
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权评价该订单");
        }

        // 2. 校验订单状态（仅已完成/已送达可评价）
        if (!REVIEWABLE_STATUSES.contains(order.getOrderStatus())) {
            throw new BusinessException(400, "当前订单状态[" + order.getOrderStatus()
                    + "]不允许评价，仅已送达/已完成订单可评价");
        }

        // 3. 防重复评价（UNIQUE 约束兜底）
        merchantReviewRepository.findByOrderIdAndUserId(orderId, userId)
                .ifPresent(r -> {
                    throw new BusinessException(400, "您已对该订单的商家进行过评价（评分：" + r.getRating() + "分）");
                });

        // 4. 保存评价
        MerchantReview review = new MerchantReview(orderId, userId, rating, comment);
        review = merchantReviewRepository.save(review);

        log.info("商家评价提交成功：reviewId={}, orderId={}, userId={}, merchantId={}, rating={}",
                review.getId(), orderId, userId, order.getMerchantId(), rating);

        // 5. 更新商家平均评分
        updateMerchantRating(order.getMerchantId());

        return buildMerchantReviewResponse(review, order);
    }

    /**
     * 查询指定商家的评价列表（分页，公开浏览）
     */
    public Page<MerchantReviewResponse> getMerchantReviews(Integer merchantId, Pageable pageable) {
        Page<MerchantReview> page = merchantReviewRepository.findByMerchantId(merchantId, pageable);

        return page.map(review -> {
            Order order = orderRepository.findById(review.getOrderId()).orElse(null);
            return buildMerchantReviewResponse(review, order);
        });
    }

    /**
     * 查询当前用户的所有商家评价（分页）
     */
    public Page<MerchantReviewResponse> getMyMerchantReviews(Integer userId, Pageable pageable) {
        Page<MerchantReview> page = merchantReviewRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return page.map(review -> {
            Order order = orderRepository.findById(review.getOrderId()).orElse(null);
            return buildMerchantReviewResponse(review, order);
        });
    }

    // ==================== 骑手评价 ====================

    /**
     * 提交骑手评价
     * 
     * 业务规则：
     * - 仅订单归属用户可评价
     * - 订单必须已分配骑手（rider_id 不为空）
     * - 订单状态必须为 delivered 或 completed
     * - 同一用户对同一订单的骑手只能评价一次
     * - 评价后自动更新骑手平均评分
     *
     * @param userId  当前登录用户ID
     * @param orderId 订单ID
     * @param rating  评分（1-5）
     * @param comment 评论文本
     * @return 评价响应
     */
    @Transactional
    public RiderReviewResponse submitRiderReview(Integer userId, Integer orderId,
                                                  Integer rating, String comment) {
        // 0. 评分范围校验（1-5分）
        if (rating == null || rating < 1 || rating > 5) {
            throw new BusinessException(400, "评分必须在1-5分之间");
        }

        // 1. 校验订单
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权评价该订单");
        }

        // 2. 校验是否分配了骑手
        if (order.getRiderId() == null) {
            throw new BusinessException(400, "该订单未分配骑手，无法评价骑手");
        }

        // 3. 校验订单状态
        if (!REVIEWABLE_STATUSES.contains(order.getOrderStatus())) {
            throw new BusinessException(400, "当前订单状态[" + order.getOrderStatus()
                    + "]不允许评价，仅已送达/已完成订单可评价");
        }

        // 4. 防重复评价
        riderReviewRepository.findByOrderIdAndUserId(orderId, userId)
                .ifPresent(r -> {
                    throw new BusinessException(400, "您已对该订单的骑手进行过评价（评分：" + r.getRating() + "分）");
                });

        // 5. 保存评价
        RiderReview review = new RiderReview(orderId, userId, order.getRiderId(), rating, comment);
        review = riderReviewRepository.save(review);

        log.info("骑手评价提交成功：reviewId={}, orderId={}, userId={}, riderId={}, rating={}",
                review.getId(), orderId, userId, order.getRiderId(), rating);

        // 6. 更新骑手平均评分
        updateRiderRating(order.getRiderId());

        return buildRiderReviewResponse(review, order);
    }

    /**
     * 查询指定骑手的评价列表（分页）
     */
    public Page<RiderReviewResponse> getRiderReviews(Integer riderId, Pageable pageable) {
        Page<RiderReview> page = riderReviewRepository.findByRiderIdOrderByCreatedAtDesc(riderId, pageable);

        return page.map(review -> {
            Order order = orderRepository.findById(review.getOrderId()).orElse(null);
            return buildRiderReviewResponse(review, order);
        });
    }

    /**
     * 查询当前用户的所有骑手评价（分页）
     */
    public Page<RiderReviewResponse> getMyRiderReviews(Integer userId, Pageable pageable) {
        Page<RiderReview> page = riderReviewRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return page.map(review -> {
            Order order = orderRepository.findById(review.getOrderId()).orElse(null);
            return buildRiderReviewResponse(review, order);
        });
    }

    // ==================== 评分聚合（内部方法） ====================

    /**
     * 重新计算商家平均评分并更新 merchant_details.rating
     * 
     * 从 merchant_reviews 中汇总属于该商家（通过订单）的所有评价，
     * 计算平均值（保留2位小数），写入 merchant_details.rating。
     * 无评价时默认 5.00。
     */
    private void updateMerchantRating(Integer merchantId) {
        List<MerchantReview> reviews = merchantReviewRepository.findAllByMerchantId(merchantId);

        BigDecimal avgRating;
        if (reviews.isEmpty()) {
            avgRating = new BigDecimal("5.00");
        } else {
            double avg = reviews.stream()
                    .mapToInt(MerchantReview::getRating)
                    .average()
                    .orElse(5.0);
            avgRating = BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
        }

        Optional<MerchantDetail> merchantOpt = merchantDetailRepository.findById(merchantId);
        if (merchantOpt.isPresent()) {
            MerchantDetail merchant = merchantOpt.get();
            merchant.setRating(avgRating);
            merchantDetailRepository.save(merchant);
            log.info("商家评分已更新：merchantId={}, rating={}, 评价数={}",
                    merchantId, avgRating, reviews.size());
        }
    }

    /**
     * 重新计算骑手平均评分并更新 rider_details.rating
     * 
     * 从 rider_reviews 中汇总该骑手的所有评价，
     * 计算平均值（保留2位小数），写入 rider_details.rating。
     * 无评价时默认 5.00。
     */
    private void updateRiderRating(Integer riderId) {
        List<RiderReview> reviews = riderReviewRepository.findAllByRiderId(riderId);

        BigDecimal avgRating;
        if (reviews.isEmpty()) {
            avgRating = new BigDecimal("5.00");
        } else {
            double avg = reviews.stream()
                    .mapToInt(RiderReview::getRating)
                    .average()
                    .orElse(5.0);
            avgRating = BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
        }

        Optional<RiderDetail> riderOpt = riderDetailRepository.findById(riderId);
        if (riderOpt.isPresent()) {
            RiderDetail rider = riderOpt.get();
            rider.setRating(avgRating);
            riderDetailRepository.save(rider);
            log.info("骑手评分已更新：riderId={}, rating={}, 评价数={}",
                    riderId, avgRating, reviews.size());
        }
    }

    // ==================== 响应构建（内部方法） ====================

    /**
     * 构建商家评价响应DTO
     */
    private MerchantReviewResponse buildMerchantReviewResponse(MerchantReview review, Order order) {
        MerchantReviewResponse resp = new MerchantReviewResponse();
        resp.setId(review.getId());
        resp.setOrderId(review.getOrderId());
        resp.setUserId(review.getUserId());
        resp.setRating(review.getRating());
        resp.setComment(review.getComment());
        resp.setCreatedAt(review.getCreatedAt());

        // 评价人用户名
        userRepository.findById(review.getUserId())
                .ifPresent(u -> resp.setUsername(u.getUsername()));

        if (order != null) {
            resp.setOrderStatus(order.getOrderStatus());

            // 商家名称
            merchantDetailRepository.findById(order.getMerchantId())
                    .ifPresent(m -> resp.setMerchantName(m.getShopName()));
        }

        return resp;
    }

    /**
     * 构建骑手评价响应DTO
     */
    private RiderReviewResponse buildRiderReviewResponse(RiderReview review, Order order) {
        RiderReviewResponse resp = new RiderReviewResponse();
        resp.setId(review.getId());
        resp.setOrderId(review.getOrderId());
        resp.setUserId(review.getUserId());
        resp.setRiderId(review.getRiderId());
        resp.setRating(review.getRating());
        resp.setComment(review.getComment());
        resp.setCreatedAt(review.getCreatedAt());

        // 评价人用户名
        userRepository.findById(review.getUserId())
                .ifPresent(u -> resp.setUsername(u.getUsername()));

        // 骑手姓名
        riderDetailRepository.findById(review.getRiderId())
                .ifPresent(r -> resp.setRiderName(r.getRealName()));

        if (order != null) {
            resp.setOrderStatus(order.getOrderStatus());
        }

        return resp;
    }
}
