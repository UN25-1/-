package com.example.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体 —— 映射 orders 表
 * 8种状态流转：pending_payment → pending → preparing → prepared → delivering → delivered → completed，另支持 cancelled / rejected / exception
 * 使用 @Version 乐观锁防止并发抢单冲突
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** 乐观锁版本号（用于防止骑手并发抢单竞态条件） */
    @Version
    @Column(name = "version", columnDefinition = "INT DEFAULT 0")
    private Integer version;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "merchant_id", nullable = false)
    private Integer merchantId;

    @Column(name = "rider_id")
    private Integer riderId;

    @Column(name = "order_status", nullable = false,
            columnDefinition = "ENUM('pending_payment','pending','preparing','prepared','delivering','delivered','completed','cancelled')")
    private String orderStatus;

    @Column(name = "total_amount", nullable = false, precision = 8, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "delivery_fee", precision = 5, scale = 2)
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    @Column(name = "delivery_address", length = 255)
    private String deliveryAddress;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "contact_name", length = 50)
    private String contactName;

    @Column(length = 255)
    private String note;

    /** 库存是否已恢复（防止取消/拒单时重复恢复库存） */
    @Column(name = "stock_restored", columnDefinition = "TINYINT DEFAULT 0")
    private Boolean stockRestored = false;

    @Column(name = "created_at", updatable = false, insertable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    public Order() {}

    public Order(Integer userId, Integer merchantId, String orderStatus,
                 BigDecimal totalAmount, BigDecimal deliveryFee,
                 String deliveryAddress, String contactPhone, String contactName, String note) {
        this.userId = userId;
        this.merchantId = merchantId;
        this.orderStatus = orderStatus;
        this.totalAmount = totalAmount;
        this.deliveryFee = deliveryFee;
        this.deliveryAddress = deliveryAddress;
        this.contactPhone = contactPhone;
        this.contactName = contactName;
        this.note = note;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    /** 乐观锁版本号 */
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getMerchantId() { return merchantId; }
    public void setMerchantId(Integer merchantId) { this.merchantId = merchantId; }

    public Integer getRiderId() { return riderId; }
    public void setRiderId(Integer riderId) { this.riderId = riderId; }

    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }

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

    /** 库存恢复标识 */
    public Boolean getStockRestored() { return stockRestored; }
    public void setStockRestored(Boolean stockRestored) { this.stockRestored = stockRestored; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
