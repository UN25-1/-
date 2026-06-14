package com.example.demo.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 更新用户信息请求 DTO
 */
public class UpdateUserRequest {

    @Size(min = 11, max = 20, message = "手机号长度需在11-20位之间")
    @Pattern(regexp = "^[0-9]+$", message = "手机号只能包含数字")
    private String phone;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
