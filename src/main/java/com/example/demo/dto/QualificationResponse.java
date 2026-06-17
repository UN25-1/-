package com.example.demo.dto;

import com.example.demo.entity.Qualification;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class QualificationResponse {

    private Integer id;
    private Integer userId;
    private String username;
    private String userRole;
    private String docType;
    private String docUrl;
    private String reviewStatus;
    private String reviewRemark;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;

    // 按申请人分组视图
    private List<QualificationResponse> documents;

    public static QualificationResponse from(Qualification q) {
        QualificationResponse r = new QualificationResponse();
        r.id = q.getId();
        r.userId = q.getUserId();
        r.userRole = q.getUserRole();
        r.docType = q.getDocType();
        r.docUrl = q.getDocUrl();
        r.reviewStatus = q.getReviewStatus();
        r.reviewRemark = q.getReviewRemark();
        r.reviewedAt = q.getReviewedAt();
        r.createdAt = q.getCreatedAt();
        return r;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
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
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<QualificationResponse> getDocuments() { return documents; }
    public void setDocuments(List<QualificationResponse> documents) { this.documents = documents; }
}
