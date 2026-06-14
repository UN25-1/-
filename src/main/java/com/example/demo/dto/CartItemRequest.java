package com.example.demo.dto;

import jakarta.validation.constraints.Min;

/**
 * 购物车 请求DTO —— 添加/修改购物车商品
 * productId 在 POST 添加时必传（由 Controller 校验），PUT 修改时从 URL 路径获取
 */
public class CartItemRequest {

    /** 商品ID：POST添加时必传，PUT修改时从路径获取（此处不做@NotNull约束） */
    private Integer productId;

    @Min(value = 1, message = "数量至少为1")
    private Integer quantity = 1;

    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
