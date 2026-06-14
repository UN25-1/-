package com.example.demo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import java.sql.Types;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, columnDefinition = "ENUM('user','rider','merchant','admin')")
    private String role;

    // status: 0=禁用, 1=正常启用, 2=待审核
    @JdbcTypeCode(Types.TINYINT)
    @Column(columnDefinition = "TINYINT DEFAULT 1 COMMENT '0=禁用, 1=启用, 2=待审核'")
    private Integer status;

    @Column(name = "created_at", updatable = false, insertable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    public User() {}

    /**
     * 带参构造：普通用户/管理员注册后直接启用(1)，商家/骑手注册后待审核(2)
     */
    public User(String username, String password, String phone, String role) {
        this.username = username;
        this.password = password;
        this.phone = phone;
        this.role = role;
        // 商家和骑手需要管理员审核，普通用户和管理员直接启用
        this.status = ("merchant".equals(role) || "rider".equals(role)) ? 2 : 1;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
