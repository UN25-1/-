package com.example.demo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDateTime;

/**
 * 骑手详情实体 —— 映射 rider_details 表
 * 一个骑手用户(user_id)只能有一条骑手档案
 */
@Entity
@Table(name = "rider_details")
public class RiderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Integer userId;

    @Column(name = "real_name", length = 50)
    private String realName;

    @Column(name = "id_card", length = 18)
    private String idCard;

    @Column(length = 50)
    private String vehicle;

    @Column(name = "vehicle_number", length = 20)
    private String vehicleNumber;

    @Column(nullable = false,
            columnDefinition = "ENUM('offline','online','busy') DEFAULT 'offline'")
    private String status = "offline";

    @Column(precision = 3, scale = 2)
    private BigDecimal rating = new BigDecimal("5.00");

    @Column(name = "completed_orders")
    private Integer completedOrders = 0;

    @JdbcTypeCode(Types.TINYINT)
    @Column(columnDefinition = "TINYINT DEFAULT 1")
    private Boolean enabled = true;

    @Column(name = "created_at", updatable = false, insertable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    public RiderDetail() {}

    public RiderDetail(Integer userId) {
        this.userId = userId;
        this.status = "offline";
        this.rating = new BigDecimal("5.00");
        this.completedOrders = 0;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }

    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }

    public String getVehicle() { return vehicle; }
    public void setVehicle(String vehicle) { this.vehicle = vehicle; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }

    public Integer getCompletedOrders() { return completedOrders; }
    public void setCompletedOrders(Integer completedOrders) { this.completedOrders = completedOrders; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
