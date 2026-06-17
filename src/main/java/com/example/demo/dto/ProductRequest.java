package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * 商品 请求DTO
 */
public class ProductRequest {

    private Integer categoryId;

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 100, message = "商品名称最多100个字符")
    private String name;

    private String description;

    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "商品价格必须大于0")
    @DecimalMax(value = "99999.99", message = "商品价格超出上限")
    private BigDecimal price;

    private String imageUrl;

    private Boolean isAvailable;

    @Min(value = 0, message = "库存不能为负数")
    @Max(value = 999999, message = "库存超出上限")
    private Integer stock;

    // ========== 分量信息（必填） ==========

    /** 分量值，如 500、250 */
    @NotBlank(message = "分量不能为空，请填写净含量")
    @JsonAlias({"portionValue", "portion_value"})
    private String portionValue;

    /** 分量单位，如 g、kg、ml、L、份 */
    @NotBlank(message = "分量单位不能为空")
    @JsonAlias({"portionUnit", "portion_unit"})
    private String portionUnit;

    /** 规格说明，如 "1袋"、"2瓶装" */
    @JsonAlias({"portionSpec", "portion_spec"})
    private String portionSpec;

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

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

    public String getPortionValue() { return portionValue; }
    public void setPortionValue(String portionValue) { this.portionValue = portionValue; }

    public String getPortionUnit() { return portionUnit; }
    public void setPortionUnit(String portionUnit) { this.portionUnit = portionUnit; }

    public String getPortionSpec() { return portionSpec; }
    public void setPortionSpec(String portionSpec) { this.portionSpec = portionSpec; }
}
