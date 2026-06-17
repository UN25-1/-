package com.example.demo.dto;

import com.example.demo.entity.Product;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品 响应DTO
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductResponse {

    private Integer id;
    private Integer merchantId;
    private String merchantName;
    private Integer categoryId;
    private String categoryName;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private Boolean isAvailable;
    private Integer stock;
    private LocalDateTime createdAt;

    // 分量信息（从 description 中解析）
    private String portionValue;
    private String portionUnit;
    private String portionSpec;

    public static ProductResponse from(Product product) {
        ProductResponse resp = new ProductResponse();
        resp.setId(product.getId());
        resp.setMerchantId(product.getMerchantId());
        resp.setCategoryId(product.getCategoryId());
        resp.setName(product.getName());
        resp.setDescription(product.getDescription());
        resp.setPrice(product.getPrice());
        resp.setImageUrl(product.getImageUrl());
        resp.setIsAvailable(product.getIsAvailable());
        resp.setStock(product.getStock());
        resp.setCreatedAt(product.getCreatedAt());
        return resp;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getMerchantId() { return merchantId; }
    public void setMerchantId(Integer merchantId) { this.merchantId = merchantId; }

    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getPortionValue() { return portionValue; }
    public void setPortionValue(String portionValue) { this.portionValue = portionValue; }

    public String getPortionUnit() { return portionUnit; }
    public void setPortionUnit(String portionUnit) { this.portionUnit = portionUnit; }

    public String getPortionSpec() { return portionSpec; }
    public void setPortionSpec(String portionSpec) { this.portionSpec = portionSpec; }
}
