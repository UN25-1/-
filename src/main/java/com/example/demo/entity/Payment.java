package com.example.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付实体 —— 映射 payments 表
 * 
 * 支付状态流转：pending → success / failed，另支持 refunding → refunded 退款流程
 * 支付方式：wechat(微信) / alipay(支付宝) / card(银行卡) / cash(货到付款)
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Integer orderId;

    @Column(name = "pay_method", nullable = false,
            columnDefinition = "ENUM('wechat','alipay','card','cash')")
    private String payMethod;

    @Column(name = "pay_status", nullable = false,
            columnDefinition = "ENUM('pending','success','failed','refunding','refunded')")
    private String payStatus;

    @Column(name = "transaction_no", length = 100)
    private String transactionNo;

    @Column(name = "paid_amount", nullable = false, precision = 8, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    public Payment() {}

    public Payment(Integer orderId, String payMethod, BigDecimal paidAmount) {
        this.orderId = orderId;
        this.payMethod = payMethod;
        this.payStatus = "pending";
        this.paidAmount = paidAmount;
    }

    // ---- Getters / Setters ----

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public String getPayMethod() { return payMethod; }
    public void setPayMethod(String payMethod) { this.payMethod = payMethod; }

    public String getPayStatus() { return payStatus; }
    public void setPayStatus(String payStatus) { this.payStatus = payStatus; }

    public String getTransactionNo() { return transactionNo; }
    public void setTransactionNo(String transactionNo) { this.transactionNo = transactionNo; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public LocalDateTime getRefundedAt() { return refundedAt; }
    public void setRefundedAt(LocalDateTime refundedAt) { this.refundedAt = refundedAt; }
}
