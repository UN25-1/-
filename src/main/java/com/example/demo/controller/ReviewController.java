package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.MerchantReviewRequest;
import com.example.demo.dto.MerchantReviewResponse;
import com.example.demo.dto.RiderReviewRequest;
import com.example.demo.dto.RiderReviewResponse;
import com.example.demo.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 评价控制器 —— 商家评价 + 骑手评价提交与查询
 * 
 * 路径：/api/review
 * - POST 评价提交需要登录
 * - GET 商家评价列表支持公开访问（商家详情页展示）
 * - GET 骑手评价列表需要登录
 */
@RestController
@RequestMapping("/api/review")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    private Integer getCurrentUserId() {
        return (Integer) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // ==================== 商家评价 ====================

    /**
     * 提交商家评价
     * POST /api/review/merchant
     * 
     * 仅订单归属用户可评价，订单状态必须为 delivered/completed，
     * 同一订单对同一商家只能评价一次。
     * 
     * 请求示例：{"orderId": 1, "rating": 5, "comment": "味道很好！"}
     */
    @PostMapping("/merchant")
    public ResponseEntity<ApiResponse<MerchantReviewResponse>> submitMerchantReview(
            @Valid @RequestBody MerchantReviewRequest request) {
        MerchantReviewResponse review = reviewService.submitMerchantReview(
                getCurrentUserId(), request.getOrderId(), request.getRating(), request.getComment());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("商家评价提交成功", review));
    }

    /**
     * 查询指定商家的评价列表（分页，按时间倒序）
     * GET /api/review/merchant/{merchantId}?page=0&size=10
     * 
     * 用于商家详情页展示评价，支持分页
     */
    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMerchantReviews(
            @PathVariable Integer merchantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<MerchantReviewResponse> reviewPage = reviewService.getMerchantReviews(merchantId, pageRequest);

        Map<String, Object> result = Map.of(
                "content", reviewPage.getContent(),
                "totalElements", reviewPage.getTotalElements(),
                "totalPages", reviewPage.getTotalPages(),
                "currentPage", reviewPage.getNumber(),
                "size", reviewPage.getSize()
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 查询我的商家评价列表（分页）
     * GET /api/review/merchant/my?page=0&size=10
     */
    @GetMapping("/merchant/my")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyMerchantReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<MerchantReviewResponse> reviewPage = reviewService.getMyMerchantReviews(
                getCurrentUserId(), pageRequest);

        Map<String, Object> result = Map.of(
                "content", reviewPage.getContent(),
                "totalElements", reviewPage.getTotalElements(),
                "totalPages", reviewPage.getTotalPages(),
                "currentPage", reviewPage.getNumber(),
                "size", reviewPage.getSize()
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ==================== 骑手评价 ====================

    /**
     * 提交骑手评价
     * POST /api/review/rider
     * 
     * 仅订单归属用户可评价，订单必须已分配骑手且状态为 delivered/completed，
     * 同一订单对同一骑手只能评价一次。
     * 
     * 请求示例：{"orderId": 1, "rating": 5, "comment": "骑手速度很快！"}
     */
    @PostMapping("/rider")
    public ResponseEntity<ApiResponse<RiderReviewResponse>> submitRiderReview(
            @Valid @RequestBody RiderReviewRequest request) {
        RiderReviewResponse review = reviewService.submitRiderReview(
                getCurrentUserId(), request.getOrderId(), request.getRating(), request.getComment());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("骑手评价提交成功", review));
    }

    /**
     * 查询指定骑手的评价列表（分页，按时间倒序）
     * GET /api/review/rider/{riderId}?page=0&size=10
     * 
     * 用于骑手详情页展示评价，支持分页
     */
    @GetMapping("/rider/{riderId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRiderReviews(
            @PathVariable Integer riderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<RiderReviewResponse> reviewPage = reviewService.getRiderReviews(riderId, pageRequest);

        Map<String, Object> result = Map.of(
                "content", reviewPage.getContent(),
                "totalElements", reviewPage.getTotalElements(),
                "totalPages", reviewPage.getTotalPages(),
                "currentPage", reviewPage.getNumber(),
                "size", reviewPage.getSize()
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 查询我的骑手评价列表（分页）
     * GET /api/review/rider/my?page=0&size=10
     */
    @GetMapping("/rider/my")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyRiderReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<RiderReviewResponse> reviewPage = reviewService.getMyRiderReviews(
                getCurrentUserId(), pageRequest);

        Map<String, Object> result = Map.of(
                "content", reviewPage.getContent(),
                "totalElements", reviewPage.getTotalElements(),
                "totalPages", reviewPage.getTotalPages(),
                "currentPage", reviewPage.getNumber(),
                "size", reviewPage.getSize()
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
