package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单响应DTO —— 包含订单基本信息、明细、商家名称与状态日志
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderResponse {

    private Integer id;
    private Integer userId;
    private String username;
    private Integer merchantId;
    private String merchantName;
    private Integer riderId;
    private String riderName;
    private String orderStatus;
    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal totalAmount;
    private String deliveryAddress;
    private String contactPhone;
    private String contactName;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** 剩余可取消秒数（仅 pending_payment / pending 状态时有值，超时后为 0） */
    private Long remainingCancelSeconds;
    /** 订单明细 */
    private List<OrderItemResponse> items;
    /** 订单状态流转日志 */
    private List<StatusLogEntry> statusLogs;

    /**
     * 状态日志条目（内嵌DTO）
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StatusLogEntry {
        private Integer id;
        private String fromStatus;
        private String toStatus;
        private Integer operatorId;
        private String operatorName;
        private String remark;
        private LocalDateTime createdAt;

        public static StatusLogEntry of(Integer id, String fromStatus, String toStatus,
                                         Integer operatorId, String operatorName,
                                         String remark, LocalDateTime createdAt) {
            StatusLogEntry entry = new StatusLogEntry();
            entry.id = id;
            entry.fromStatus = fromStatus;
            entry.toStatus = toStatus;
            entry.operatorId = operatorId;
            entry.operatorName = operatorName;
            entry.remark = remark;
            entry.createdAt = createdAt;
            return entry;
        }

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getFromStatus() { return fromStatus; }
        public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
        public String getToStatus() { return toStatus; }
        public void setToStatus(String toStatus) { this.toStatus = toStatus; }
        public Integer getOperatorId() { return operatorId; }
        public void setOperatorId(Integer operatorId) { this.operatorId = operatorId; }
        public String getOperatorName() { return operatorName; }
        public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Integer getMerchantId() { return merchantId; }
    public void setMerchantId(Integer merchantId) { this.merchantId = merchantId; }

    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }

    public Integer getRiderId() { return riderId; }
    public void setRiderId(Integer riderId) { this.riderId = riderId; }

    public String getRiderName() { return riderName; }
    public void setRiderName(String riderName) { this.riderName = riderName; }

    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(BigDecimal deliveryFee) { this.deliveryFee = deliveryFee; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getRemainingCancelSeconds() { return remainingCancelSeconds; }
    public void setRemainingCancelSeconds(Long remainingCancelSeconds) { this.remainingCancelSeconds = remainingCancelSeconds; }

    public List<OrderItemResponse> getItems() { return items; }
    public void setItems(List<OrderItemResponse> items) { this.items = items; }

    public List<StatusLogEntry> getStatusLogs() { return statusLogs; }
    public void setStatusLogs(List<StatusLogEntry> statusLogs) { this.statusLogs = statusLogs; }
}
