package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 更新用户信息请求 DTO
 */
public class UpdateUserRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 50, message = "用户名长度需在2-50位之间")
    @Pattern(regexp = "^[a-zA-Z0-9_\\u4e00-\\u9fa5]+$", message = "用户名只能包含中英文、数字和下划线")
    private String username;

    @Size(min = 11, max = 20, message = "手机号长度需在11-20位之间")
    @Pattern(regexp = "^[0-9]+$", message = "手机号只能包含数字")
    private String phone;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
