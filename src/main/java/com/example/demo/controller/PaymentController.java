package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.PaymentRequest;
import com.example.demo.dto.PaymentResponse;
import com.example.demo.dto.RefundRequest;
import com.example.demo.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 支付控制器 —— 发起支付 + 模拟回调 + 退款 + 支付查询
 * 
 * 路径：/api/payment/**  （所有已认证用户可访问）
 */
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    private Integer getCurrentUserId() {
        return (Integer) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // ==================== 发起支付 ====================

    /**
     * 发起支付：为待支付订单选择支付方式并创建支付记录
     * POST /api/payment/create
     * 
     * 请求示例：{"orderId": 3, "payMethod": "alipay"}
     * 货到付款(cash)将直接标记支付完成，无需后续回调
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @Valid @RequestBody PaymentRequest request) {
        PaymentResponse payment = paymentService.createPayment(
                getCurrentUserId(), request.getOrderId(), request.getPayMethod());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("支付记录创建成功", payment));
    }

    // ==================== 模拟支付回调 ====================

    /**
     * 模拟支付网关回调：支付成功
     * PUT /api/payment/{id}/pay
     * 
     * 真实环境由微信/支付宝异步回调触发，此处为开发模拟。
     * 支付成功后自动将订单状态从 pending_payment 推进至 pending。
     */
    @PutMapping("/{id}/pay")
    public ResponseEntity<ApiResponse<PaymentResponse>> payCallback(@PathVariable Integer id) {
        PaymentResponse payment = paymentService.simulatePayCallback(id, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("支付成功", payment));
    }

    // ==================== 快捷支付 ====================

    /**
     * 快捷支付：一键发起支付并模拟回调（兼容旧接口）
     * POST /api/payment/quick-pay
     * 
     * 默认微信支付，等同于 PUT /api/orders/{id}/pay
     * 请求示例：{"orderId": 3}
     */
    @PostMapping("/quick-pay")
    public ResponseEntity<ApiResponse<PaymentResponse>> quickPay(
            @Valid @RequestBody PaymentRequest request) {
        PaymentResponse payment = paymentService.quickPay(request.getOrderId(), getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("支付成功", payment));
    }

    // ==================== 退款 ====================

    /**
     * 申请退款（用户或商户发起）
     * POST /api/payment/refund
     * 
     * 仅支付成功(success)状态的记录可退款
     * 货到付款(cash)无需退款
     */
    @PostMapping("/refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> refund(
            @Valid @RequestBody RefundRequest request) {
        PaymentResponse payment = paymentService.processRefund(
                request.getOrderId(), getCurrentUserId(), request.getReason());
        if (payment == null) {
            return ResponseEntity.ok(ApiResponse.success("该订单无需退款（无支付记录或货到付款）", null));
        }
        return ResponseEntity.ok(ApiResponse.success("退款处理完成", payment));
    }

    // ==================== 支付查询 ====================

    /**
     * 查询我的支付记录（分页）
     * GET /api/payment/my?page=0&size=10
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "paidAt"));
        Page<PaymentResponse> paymentPage = paymentService.getMyPayments(getCurrentUserId(), pageRequest);

        Map<String, Object> result = Map.of(
                "content", paymentPage.getContent(),
                "totalElements", paymentPage.getTotalElements(),
                "totalPages", paymentPage.getTotalPages(),
                "currentPage", paymentPage.getNumber(),
                "size", paymentPage.getSize()
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 查询单条支付详情
     * GET /api/payment/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentDetail(@PathVariable Integer id) {
        PaymentResponse payment = paymentService.getPaymentDetail(id, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(payment));
    }

    /**
     * 根据订单ID查询支付记录
     * GET /api/payment/order/{orderId}
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrder(
            @PathVariable Integer orderId) {
        PaymentResponse payment = paymentService.getPaymentByOrderId(orderId, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(payment));
    }
}
