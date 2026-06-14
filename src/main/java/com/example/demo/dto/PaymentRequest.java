package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 发起支付请求 DTO
 */
public class PaymentRequest {

    @NotNull(message = "订单ID不能为空")
    private Integer orderId;

    @NotBlank(message = "支付方式不能为空")
    @Pattern(regexp = "wechat|alipay|card|cash", message = "支付方式仅支持 wechat/alipay/card/cash")
    private String payMethod;

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public String getPayMethod() { return payMethod; }
    public void setPayMethod(String payMethod) { this.payMethod = payMethod; }
}
