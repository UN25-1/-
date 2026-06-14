package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车按商家分组展示
 * 将购物车中的商品按所属商家聚合，方便用户检查跨商家商品
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartMerchantGroup {

    /** 商家ID */
    private Integer merchantId;
    /** 商家名称 */
    private String merchantName;
    /** 该商家下的购物车商品列表 */
    private List<CartItemResponse> items;
    /** 该商家的商品小计（确保不为null，防止前端JSON反序列化时出现NaN） */
    private BigDecimal subtotal = BigDecimal.ZERO;
    /** 配送费（确保不为null） */
    private BigDecimal deliveryFee = BigDecimal.ZERO;
    /** 起送价（确保不为null） */
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    public Integer getMerchantId() { return merchantId; }
    public void setMerchantId(Integer merchantId) { this.merchantId = merchantId; }

    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }

    public List<CartItemResponse> getItems() { return items; }
    public void setItems(List<CartItemResponse> items) { this.items = items; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(BigDecimal deliveryFee) { this.deliveryFee = deliveryFee; }

    public BigDecimal getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(BigDecimal minOrderAmount) { this.minOrderAmount = minOrderAmount; }
}
