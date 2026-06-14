package com.example.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.hibernate.annotations.JdbcTypeCode;
import java.sql.Types;
import java.time.LocalDateTime;

/**
 * 骑手评价实体 —— 映射 rider_reviews 表
 * 
 * 用户对骑手的评价（1-5分 + 文字评论），每笔订单对同一骑手只能评价一次
 * 评价提交后自动触发骑手评分聚合更新 rider_details.rating
 */
@Entity
@Table(name = "rider_reviews")
public class RiderReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "order_id", nullable = false)
    private Integer orderId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "rider_id", nullable = false)
    private Integer riderId;

    @JdbcTypeCode(Types.TINYINT)
    @Min(1)
    @Max(5)
    @Column(nullable = false, columnDefinition = "TINYINT")
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at", updatable = false, insertable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    public RiderReview() {}

    public RiderReview(Integer orderId, Integer userId, Integer riderId, Integer rating, String comment) {
        this.orderId = orderId;
        this.userId = userId;
        this.riderId = riderId;
        this.rating = rating;
        this.comment = comment;
    }

    // ---- Getters / Setters ----

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getRiderId() { return riderId; }
    public void setRiderId(Integer riderId) { this.riderId = riderId; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
