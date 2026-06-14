package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 用户注册请求 DTO
 * 使用 Jakarta Validation 进行参数校验，增强安全性
 */
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度需在3-50个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_\\u4e00-\\u9fff]+$", message = "用户名只能包含中文、字母、数字和下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度需在6-100个字符之间")
    private String password;

    @Size(max = 20, message = "手机号长度不能超过20位")
    @Pattern(regexp = "^[0-9]*$", message = "手机号只能包含数字")
    private String phone;

    // 默认注册为用户角色
    private String role = "user";

    public RegisterRequest() {}

    public RegisterRequest(String username, String password, String phone) {
        this.username = username;
        this.password = password;
        this.phone = phone;
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
}
