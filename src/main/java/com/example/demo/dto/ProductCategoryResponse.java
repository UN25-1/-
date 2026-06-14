package com.example.demo.dto;

import com.example.demo.entity.ProductCategory;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 菜品分类 响应DTO
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductCategoryResponse {

    private Integer id;
    private Integer merchantId;
    private String name;
    private Integer sortOrder;
    /** 上架/下架：true=上架, false=下架 */
    private Boolean isAvailable;

    public static ProductCategoryResponse from(ProductCategory category) {
        ProductCategoryResponse resp = new ProductCategoryResponse();
        resp.setId(category.getId());
        resp.setMerchantId(category.getMerchantId());
        resp.setName(category.getName());
        resp.setSortOrder(category.getSortOrder());
        resp.setIsAvailable(category.getIsAvailable());
        return resp;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getMerchantId() { return merchantId; }
    public void setMerchantId(Integer merchantId) { this.merchantId = merchantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }
}
