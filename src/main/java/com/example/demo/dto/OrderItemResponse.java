package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/**
 * 订单明细响应DTO —— 包含商品信息与金额
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderItemResponse {

    /** 订单明细ID */
    private Integer id;
    /** 商品ID */
    private Integer productId;
    /** 商品名称 */
    private String productName;
    /** 商品图片 */
    private String productImageUrl;
    /** 下单时单价 */
    private BigDecimal price;
    /** 数量 */
    private Integer quantity;
    /** 小计 */
    private BigDecimal subtotal;

    public static OrderItemResponse of(Integer id, Integer productId, String productName,
                                        String productImageUrl, BigDecimal price,
                                        Integer quantity) {
        OrderItemResponse resp = new OrderItemResponse();
        resp.id = id;
        resp.productId = productId;
        resp.productName = productName;
        resp.productImageUrl = productImageUrl;
        resp.price = price;
        resp.quantity = quantity;
        resp.subtotal = price.multiply(BigDecimal.valueOf(quantity));
        return resp;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductImageUrl() { return productImageUrl; }
    public void setProductImageUrl(String productImageUrl) { this.productImageUrl = productImageUrl; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}
