package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 地址请求 DTO（新增/编辑共用）
 * 兼容驼峰(camelCase)和蛇形(snake_case)两种命名风格
 */
public class AddressRequest {

    @NotBlank(message = "联系人姓名不能为空")
    @Size(max = 50, message = "联系人姓名最长50个字符")
    @JsonProperty("contact_name")
    @JsonAlias({"contactName", "receiverName", "receiver_name", "name"})
    private String contactName;

    @NotBlank(message = "联系电话不能为空")
    @Size(min = 11, max = 20, message = "联系电话长度需在11-20位之间")
    @Pattern(regexp = "^[0-9]+$", message = "联系电话只能包含数字")
    @JsonProperty("phone")
    @JsonAlias({"phone", "mobile", "phoneNumber", "phone_number"})
    private String phone;

    @NotBlank(message = "地址不能为空")
    @Size(max = 255, message = "地址最长255个字符")
    @JsonProperty("address")
    @JsonAlias({"address", "region", "area", "detailAddress", "detail_address"})
    private String address;

    @JsonProperty("is_default")
    @JsonAlias({"isDefault", "is_default"})
    private Boolean isDefault;  // 是否设为默认地址

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
}
