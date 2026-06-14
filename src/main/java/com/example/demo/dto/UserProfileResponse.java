package com.example.demo.dto;

import java.time.LocalDateTime;

/**
 * 用户信息 响应DTO（管理员视角，含 enabled/role/status 等敏感字段）
 */
public class UserProfileResponse {

    private Integer id;
    private String username;
    private String phone;
    private String role;
    /** 兼容字段：status==1 时为 true */
    private Boolean enabled;
    /** 原始状态值：0=禁用, 1=启用, 2=待审核 */
    private Integer status;
    private LocalDateTime createdAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
