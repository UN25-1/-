package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.service.RiderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 骑手控制器 —— 骑手认证 + 在线状态 + 接单配送 + 订单查询
 *
 * 所有接口均需 ROLE_RIDER 权限，通过 SecurityConfig 中 "/api/rider/**" 控制
 */
@RestController
@RequestMapping("/api/rider")
public class RiderController {

    private final RiderService riderService;

    public RiderController(RiderService riderService) {
        this.riderService = riderService;
    }

    private Integer getCurrentUserId() {
        return (Integer) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // ==================== 骑手档案 ====================

    /**
     * 查看骑手档案
     * GET /api/rider/profile
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<RiderDetailResponse>> getProfile() {
        RiderDetailResponse profile = riderService.getProfile(getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    /**
     * 完善/更新骑手档案（认证信息）
     * PUT /api/rider/profile
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<RiderDetailResponse>> updateProfile(
            @Valid @RequestBody RiderDetailRequest request) {
        RiderDetailResponse profile = riderService.updateProfile(getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("骑手信息已更新", profile));
    }

    // ==================== 在线状态 ====================

    /**
     * 切换在线状态（offline / online / busy）
     * PUT /api/rider/status
     */
    @PutMapping("/status")
    public ResponseEntity<ApiResponse<RiderDetailResponse>> updateStatus(
            @Valid @RequestBody RiderStatusRequest request) {
        RiderDetailResponse profile = riderService.updateStatus(getCurrentUserId(), request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("状态已切换为 " + request.getStatus(), profile));
    }

    // ==================== 可接订单 ====================

    /**
     * 查看可接订单池（pending/preparing 且无骑手接单的订单）
     * GET /api/rider/orders/available
     */
    @GetMapping("/orders/available")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAvailableOrders() {
        List<OrderResponse> orders = riderService.getAvailableOrders();
        return ResponseEntity.ok(ApiResponse.success("共" + orders.size() + "个可接订单", orders));
    }

    // ==================== 接单 ====================

    /**
     * 骑手抢单
     * PUT /api/rider/orders/{id}/grab
     */
    @PutMapping("/orders/{id}/grab")
    public ResponseEntity<ApiResponse<OrderResponse>> grabOrder(@PathVariable Integer id) {
        OrderResponse order = riderService.grabOrder(id, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("接单成功", order));
    }

    // ==================== 取餐 ====================

    /**
     * 骑手到店取餐：preparing → delivering
     * PUT /api/rider/orders/{id}/pickup
     */
    @PutMapping("/orders/{id}/pickup")
    public ResponseEntity<ApiResponse<OrderResponse>> pickupOrder(@PathVariable Integer id) {
        OrderResponse order = riderService.pickupOrder(id, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("已取餐，开始配送", order));
    }

    // ==================== 送达 ====================

    /**
     * 骑手送达：delivering → delivered
     * PUT /api/rider/orders/{id}/deliver
     */
    @PutMapping("/orders/{id}/deliver")
    public ResponseEntity<ApiResponse<OrderResponse>> deliverOrder(@PathVariable Integer id) {
        OrderResponse order = riderService.deliverOrder(id, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("已送达", order));
    }

    /**
     * 骑手上报配送异常（如用户失联、地址错误等）
     * PUT /api/rider/orders/{id}/exception
     */
    @PutMapping("/orders/{id}/exception")
    public ResponseEntity<ApiResponse<OrderResponse>> reportException(
            @PathVariable Integer id,
            @RequestParam(required = false) String reason) {
        OrderResponse order = riderService.reportException(id, getCurrentUserId(), reason);
        return ResponseEntity.ok(ApiResponse.success("配送异常已上报，订单标记为异常状态", order));
    }

    // ==================== 骑手收入 ====================

    /**
     * 获取骑手收入统计（累计 + 今日 + 完成单数）
     * GET /api/rider/earnings
     */
    @GetMapping("/earnings")
    public ResponseEntity<ApiResponse<RiderEarningsResponse>> getEarnings() {
        RiderEarningsResponse earnings = riderService.getEarnings(getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(earnings));
    }

    // ==================== 骑手订单查询 ====================

    /**
     * 骑手查询自己的配送订单（支持按状态筛选 + 分页）
     * GET /api/rider/orders?status=delivering&page=0&size=10
     */
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<OrderResponse> orderPage = riderService.getMyOrders(getCurrentUserId(), status, pageRequest);

        Map<String, Object> result = Map.of(
                "content", orderPage.getContent(),
                "totalElements", orderPage.getTotalElements(),
                "totalPages", orderPage.getTotalPages(),
                "currentPage", orderPage.getNumber(),
                "size", orderPage.getSize()
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
