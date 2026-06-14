package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 管理员操作审计日志实体 —— 映射 audit_logs 表
 * 记录所有管理员的敏感操作，用于安全合规追溯
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 操作目标类型：USER / MERCHANT / RIDER / ORDER */
    @Column(name = "target_type", nullable = false, length = 30)
    private String targetType;

    /** 操作目标ID */
    @Column(name = "target_id")
    private Integer targetId;

    /** 操作类型：UPDATE_STATUS / CREATE / DELETE 等 */
    @Column(nullable = false, length = 50)
    private String action;

    /** 操作详情（如 "status: 1 → 0"、"enabled: true → false"） */
    @Column(columnDefinition = "TEXT")
    private String detail;

    /** 操作目标名称（冗余字段，方便查询） */
    @Column(name = "target_name", length = 100)
    private String targetName;

    /** 操作人ID（管理员用户ID） */
    @Column(name = "operator_id")
    private Integer operatorId;

    /** 操作人IP */
    @Column(name = "operator_ip", length = 45)
    private String operatorIp;

    @Column(name = "created_at", updatable = false, insertable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    public AuditLog() {}

    public AuditLog(String targetType, Integer targetId, String action,
                    String detail, String targetName, Integer operatorId) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.action = action;
        this.detail = detail;
        this.targetName = targetName;
        this.operatorId = operatorId;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public Integer getTargetId() { return targetId; }
    public void setTargetId(Integer targetId) { this.targetId = targetId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }

    public Integer getOperatorId() { return operatorId; }
    public void setOperatorId(Integer operatorId) { this.operatorId = operatorId; }

    public String getOperatorIp() { return operatorIp; }
    public void setOperatorIp(String operatorIp) { this.operatorIp = operatorIp; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
