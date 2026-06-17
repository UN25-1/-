package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "qualification_documents")
public class Qualification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "user_role", nullable = false, length = 20)
    private String userRole; // merchant / rider

    @Column(name = "doc_type", nullable = false, length = 30)
    private String docType; // business_license / id_card / health_cert

    @Column(name = "doc_url", nullable = false, length = 500)
    private String docUrl;

    @Column(name = "review_status", nullable = false, length = 20)
    private String reviewStatus = "pending"; // pending / approved / rejected

    @Column(name = "review_remark", length = 500)
    private String reviewRemark;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", updatable = false, insertable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    public Qualification() {}

    public Qualification(Integer userId, String userRole, String docType, String docUrl) {
        this.userId = userId;
        this.userRole = userRole;
        this.docType = docType;
        this.docUrl = docUrl;
        this.reviewStatus = "pending";
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }
    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }
    public String getDocUrl() { return docUrl; }
    public void setDocUrl(String docUrl) { this.docUrl = docUrl; }
    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
    public String getReviewRemark() { return reviewRemark; }
    public void setReviewRemark(String reviewRemark) { this.reviewRemark = reviewRemark; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
