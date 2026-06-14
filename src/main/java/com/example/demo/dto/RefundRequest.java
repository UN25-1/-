package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 退款请求 DTO
 */
public class RefundRequest {

    @NotNull(message = "订单ID不能为空")
    private Integer orderId;

    /** 退款原因（可选） */
    private String reason;

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
