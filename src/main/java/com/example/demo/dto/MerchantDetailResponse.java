package com.example.demo.dto;

import com.example.demo.entity.MerchantDetail;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 商家详情 响应DTO
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MerchantDetailResponse {

    private Integer id;
    private Integer userId;
    private String shopName;
    private String shopAddress;
    private String shopPhone;
    private String description;
    private String logoUrl;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private BigDecimal deliveryFee;
    private BigDecimal minOrderAmount;
    private BigDecimal rating;

    public static MerchantDetailResponse from(MerchantDetail detail) {
        MerchantDetailResponse resp = new MerchantDetailResponse();
        resp.setId(detail.getId());
        resp.setUserId(detail.getUserId());
        resp.setShopName(detail.getShopName());
        resp.setShopAddress(detail.getShopAddress());
        resp.setShopPhone(detail.getShopPhone());
        resp.setDescription(detail.getDescription());
        resp.setLogoUrl(detail.getLogoUrl());
        resp.setOpeningTime(detail.getOpeningTime());
        resp.setClosingTime(detail.getClosingTime());
        resp.setDeliveryFee(detail.getDeliveryFee());
        resp.setMinOrderAmount(detail.getMinOrderAmount());
        resp.setRating(detail.getRating());
        return resp;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

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

    public LocalTime getOpeningTime() { return openingTime; }
    public void setOpeningTime(LocalTime openingTime) { this.openingTime = openingTime; }

    public LocalTime getClosingTime() { return closingTime; }
    public void setClosingTime(LocalTime closingTime) { this.closingTime = closingTime; }

    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(BigDecimal deliveryFee) { this.deliveryFee = deliveryFee; }

    public BigDecimal getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(BigDecimal minOrderAmount) { this.minOrderAmount = minOrderAmount; }

    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }
}
