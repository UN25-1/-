package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车 响应DTO —— 包含商品详情与商家聚合信息
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartItemResponse {

    /** 购物车项ID */
    private Integer cartItemId;
    /** 商品ID */
    private Integer productId;
    /** 商品名称 */
    private String productName;
    /** 商品图片 */
    private String productImageUrl;
    /** 商品单价（确保不为null，防止前端JSON NaN） */
    private BigDecimal productPrice = BigDecimal.ZERO;
    /** 数量 */
    private Integer quantity;
    /** 小计金额（确保不为null，防止前端JSON NaN） */
    private BigDecimal subtotal = BigDecimal.ZERO;
    /** 所属商家ID */
    private Integer merchantId;
    /** 商家名称 */
    private String merchantName;

    /** 是否为新添加（true=首次添加到购物车，false=已有商品累加数量） */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private Boolean isNewItem;

    /** 按商家聚合后的分组展示（用于 /api/cart 列表接口） */
    private List<CartMerchantGroup> merchantGroups;

    /**
     * 构建购物车项响应（所有 BigDecimal 字段均为 null-safe，防止前端 JSON NaN）
     */
    public static CartItemResponse of(Integer cartItemId, Integer productId, String productName,
                                       String productImageUrl, BigDecimal productPrice,
                                       Integer quantity, Integer merchantId, String merchantName) {
        CartItemResponse resp = new CartItemResponse();
        resp.cartItemId = cartItemId;
        resp.productId = productId;
        resp.productName = productName;
        resp.productImageUrl = productImageUrl;
        // 防御性处理：price/quantity 为 null 时默认取 0，杜绝前端收到 undefined → NaN
        BigDecimal safePrice = productPrice != null ? productPrice : BigDecimal.ZERO;
        int safeQty = quantity != null ? quantity : 0;
        resp.productPrice = safePrice;
        resp.quantity = safeQty;
        resp.subtotal = safePrice.multiply(BigDecimal.valueOf(safeQty));
        resp.merchantId = merchantId;
        resp.merchantName = merchantName;
        return resp;
    }

    // Getters and Setters
    public Integer getCartItemId() { return cartItemId; }
    public void setCartItemId(Integer cartItemId) { this.cartItemId = cartItemId; }

    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductImageUrl() { return productImageUrl; }
    public void setProductImageUrl(String productImageUrl) { this.productImageUrl = productImageUrl; }

    public BigDecimal getProductPrice() { return productPrice; }
    public void setProductPrice(BigDecimal productPrice) { this.productPrice = productPrice; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public Integer getMerchantId() { return merchantId; }
    public void setMerchantId(Integer merchantId) { this.merchantId = merchantId; }

    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }

    public List<CartMerchantGroup> getMerchantGroups() { return merchantGroups; }
    public void setMerchantGroups(List<CartMerchantGroup> merchantGroups) { this.merchantGroups = merchantGroups; }

    public Boolean getIsNewItem() { return isNewItem; }
    public void setIsNewItem(Boolean isNewItem) { this.isNewItem = isNewItem; }
}
