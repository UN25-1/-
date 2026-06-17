package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.OrderRequest;
import com.example.demo.dto.OrderResponse;
import com.example.demo.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 订单控制器 —— 用户下单 + 状态流转 + 多角色查询
 *
 * 用户接口：/api/orders/**  （所有已认证用户）
 * 商家接口：/api/merchant/orders/** （需 MERCHANT 角色）
 */
@RestController
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    private Integer getCurrentUserId() {
        return (Integer) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // ==================== 用户下单 ====================

    /**
     * 提交订单（从购物车创建）
     * POST /api/orders
     */
    @PostMapping("/api/orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> createOrder(
            @Valid @RequestBody OrderRequest request) {
        List<OrderResponse> orders = orderService.createOrder(getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("下单成功，共生成" + orders.size() + "笔订单", orders));
    }

    // ==================== 支付（委托 PaymentService） ====================

    /**
     * 快捷支付（兼容旧接口，内部委托 PaymentService 完成支付创建+回调）
     * PUT /api/orders/{id}/pay
     */
    @PutMapping("/api/orders/{id}/pay")
    public ResponseEntity<ApiResponse<OrderResponse>> payOrder(@PathVariable Integer id) {
        OrderResponse order = orderService.payOrder(id, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("支付成功", order));
    }

    // ==================== 取消订单 ====================

    /**
     * 用户取消订单
     * PUT /api/orders/{id}/cancel
     */
    @PutMapping("/api/orders/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(@PathVariable Integer id) {
        OrderResponse order = orderService.cancelOrder(id, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("订单已取消", order));
    }

    // ==================== 确认收货 ====================

    /**
     * 用户确认收货
     * PUT /api/orders/{id}/complete
     */
    @PutMapping("/api/orders/{id}/complete")
    public ResponseEntity<ApiResponse<OrderResponse>> completeOrder(@PathVariable Integer id) {
        OrderResponse order = orderService.completeOrder(id, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("已确认收货", order));
    }

    /**
     * 用户拒收已送达订单（需人工处理退款）
     * PUT /api/orders/{id}/reject
     */
    @PutMapping("/api/orders/{id}/reject")
    public ResponseEntity<ApiResponse<OrderResponse>> rejectDelivery(@PathVariable Integer id) {
        OrderResponse order = orderService.rejectDelivery(id, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("订单已标记为拒收", order));
    }

    // ==================== 删除订单 ====================

    /**
     * 用户删除订单（仅终态可删除）
     * DELETE /api/orders/{id}
     */
    @DeleteMapping("/api/orders/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable Integer id) {
        orderService.deleteOrder(id, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("订单已删除", null));
    }

    // ==================== 查询：用户视角 ====================

    /**
     * 查询我的订单列表（支持按状态筛选 + 分页）
     * GET /api/orders?status=pending&page=0&size=10
     */
    @GetMapping("/api/orders")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<OrderResponse> orderPage = orderService.getMyOrders(getCurrentUserId(), status, pageRequest);

        Map<String, Object> result = Map.of(
                "content", orderPage.getContent(),
                "totalElements", orderPage.getTotalElements(),
                "totalPages", orderPage.getTotalPages(),
                "currentPage", orderPage.getNumber(),
                "size", orderPage.getSize()
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ==================== 订单详情 ====================

    /**
     * 获取订单详情（含明细和状态日志）
     * GET /api/orders/{id}
     */
    @GetMapping("/api/orders/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderDetail(@PathVariable Integer id) {
        OrderResponse order = orderService.getUserOrderDetail(id, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    // ==================== 商家视角：订单管理 ====================

    /**
     * 商家查询本店订单（支持按状态筛选 + 分页）
     * GET /api/merchant/orders?status=pending&page=0&size=10
     */
    @GetMapping("/api/merchant/orders")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMerchantOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<OrderResponse> orderPage = orderService.getMerchantOrders(getCurrentUserId(), status, pageRequest);

        Map<String, Object> result = Map.of(
                "content", orderPage.getContent(),
                "totalElements", orderPage.getTotalElements(),
                "totalPages", orderPage.getTotalPages(),
                "currentPage", orderPage.getNumber(),
                "size", orderPage.getSize()
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 商家查看本店订单详情（含明细和状态日志）
     * GET /api/merchant/orders/{id}
     */
    @GetMapping("/api/merchant/orders/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getMerchantOrderDetail(@PathVariable Integer id) {
        OrderResponse order = orderService.getMerchantOrderDetail(id, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    /**
     * 商家接单（备餐中）
     * PUT /api/merchant/orders/{id}/accept
     */
    @PutMapping("/api/merchant/orders/{id}/accept")
    public ResponseEntity<ApiResponse<OrderResponse>> acceptOrder(@PathVariable Integer id) {
        OrderResponse order = orderService.acceptOrder(id, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("已接单，开始备餐", order));
    }

    /**
     * 商家备餐完成
     * PUT /api/merchant/orders/{id}/prepare-complete
     */
    @PutMapping("/api/merchant/orders/{id}/prepare-complete")
    public ResponseEntity<ApiResponse<OrderResponse>> completePreparation(@PathVariable Integer id) {
        OrderResponse order = orderService.completePreparation(id, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("备餐完成，等待骑手取餐", order));
    }

    /**
     * 商家拒单（含自动退款+库存恢复）
     * PUT /api/merchant/orders/{id}/reject
     */
    @PutMapping("/api/merchant/orders/{id}/reject")
    public ResponseEntity<ApiResponse<OrderResponse>> rejectOrder(
            @PathVariable Integer id,
            @RequestParam(required = false) String reason) {
        OrderResponse order = orderService.rejectOrder(id, getCurrentUserId(), reason);
        return ResponseEntity.ok(ApiResponse.success("订单已拒单", order));
    }
}
