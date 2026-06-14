package com.example.demo.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * 商家入驻/编辑店铺信息 请求DTO
 */
public class MerchantDetailRequest {

    @NotBlank(message = "店铺名称不能为空")
    @Size(max = 100, message = "店铺名称最多100个字符")
    private String shopName;

    @Size(max = 255, message = "店铺地址最多255个字符")
    private String shopAddress;

    @Size(max = 20, message = "店铺电话最多20个字符")
    private String shopPhone;

    private String description;

    private String logoUrl;

    private String openingTime;

    private String closingTime;

    @DecimalMin(value = "0.00", message = "配送费不能为负数")
    @DecimalMax(value = "999.99", message = "配送费超出上限")
    private BigDecimal deliveryFee;

    @DecimalMin(value = "0.00", message = "起送价不能为负数")
    @DecimalMax(value = "99999.99", message = "起送价超出上限")
    private BigDecimal minOrderAmount;

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public String getShopAddress() { return shopAddress; }
    public void setShopAddress(String shopAddress) { this.shopAddress = shopAddress; }

    public String getShopPhone() { return shopPhone; }
    public void setShopPhone(String shopPhone) { this.shopPhone = shopPhone; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getOpeningTime() { return openingTime; }
    public void setOpeningTime(String openingTime) { this.openingTime = openingTime; }

    public String getClosingTime() { return closingTime; }
    public void setClosingTime(String closingTime) { this.closingTime = closingTime; }

    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(BigDecimal deliveryFee) { this.deliveryFee = deliveryFee; }

    public BigDecimal getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(BigDecimal minOrderAmount) { this.minOrderAmount = minOrderAmount; }
}
