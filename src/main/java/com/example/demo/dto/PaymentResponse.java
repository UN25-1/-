package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录响应 DTO —— 含支付详情及关联订单摘要
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponse {

    // 支付信息
    private Integer id;
    private Integer orderId;
    private String payMethod;
    private String payMethodDesc;      // 支付方式中文描述
    private String payStatus;
    private String payStatusDesc;      // 支付状态中文描述
    private String transactionNo;
    private BigDecimal paidAmount;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;

    // 关联订单摘要
    private String merchantName;
    private String orderStatus;
    private LocalDateTime orderCreatedAt;

    // ---- 静态工具方法 ----

    public static String payMethodDesc(String method) {
        return switch (method) {
            case "wechat" -> "微信支付";
            case "alipay" -> "支付宝";
            case "card"   -> "银行卡";
            case "cash"   -> "货到付款";
            default       -> method;
        };
    }

    public static String payStatusDesc(String status) {
        return switch (status) {
            case "pending"   -> "待支付";
            case "success"   -> "支付成功";
            case "failed"    -> "支付失败";
            case "refunding" -> "退款中";
            case "refunded"  -> "已退款";
            default          -> status;
        };
    }

    // ---- Getters / Setters ----

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public String getPayMethod() { return payMethod; }
    public void setPayMethod(String payMethod) {
        this.payMethod = payMethod;
        this.payMethodDesc = payMethodDesc(payMethod);
    }

    public String getPayMethodDesc() { return payMethodDesc; }

    public String getPayStatus() { return payStatus; }
    public void setPayStatus(String payStatus) {
        this.payStatus = payStatus;
        this.payStatusDesc = payStatusDesc(payStatus);
    }

    public String getPayStatusDesc() { return payStatusDesc; }

    public String getTransactionNo() { return transactionNo; }
    public void setTransactionNo(String transactionNo) { this.transactionNo = transactionNo; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public LocalDateTime getRefundedAt() { return refundedAt; }
    public void setRefundedAt(LocalDateTime refundedAt) { this.refundedAt = refundedAt; }

    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }

    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }

    public LocalDateTime getOrderCreatedAt() { return orderCreatedAt; }
    public void setOrderCreatedAt(LocalDateTime orderCreatedAt) { this.orderCreatedAt = orderCreatedAt; }
}
