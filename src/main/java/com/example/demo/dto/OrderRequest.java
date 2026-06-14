package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 下单请求DTO
 */
public class OrderRequest {

    /** 配送地址ID（来自 user_addresses 表） */
    @NotNull(message = "请选择配送地址")
    private Integer addressId;

    /** 订单备注（选填） */
    @Size(max = 255, message = "备注不能超过255个字符")
    private String note;

    public Integer getAddressId() { return addressId; }
    public void setAddressId(Integer addressId) { this.addressId = addressId; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
