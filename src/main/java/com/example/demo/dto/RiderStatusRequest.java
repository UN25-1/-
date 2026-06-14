package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 骑手在线状态 请求DTO
 */
public class RiderStatusRequest {

    @NotBlank(message = "状态不能为空")
    private String status;  // offline / online / busy

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
