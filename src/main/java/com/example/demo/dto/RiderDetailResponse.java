package com.example.demo.dto;

import com.example.demo.entity.RiderDetail;
import com.example.demo.entity.User;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/**
 * 骑手详情 响应DTO
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RiderDetailResponse {

    private Integer id;
    private Integer userId;
    private String username;
    private String phone;
    private String realName;
    private String idCard;
    private String vehicle;
    private String vehicleNumber;
    private String status;
    private BigDecimal rating;
    private Integer completedOrders;

    public static RiderDetailResponse from(RiderDetail detail, User user) {
        RiderDetailResponse resp = new RiderDetailResponse();
        resp.setId(detail.getId());
        resp.setUserId(detail.getUserId());
        if (user != null) {
            resp.setUsername(user.getUsername());
            resp.setPhone(user.getPhone());
        }
        resp.setRealName(detail.getRealName());
        resp.setIdCard(detail.getIdCard());
        resp.setVehicle(detail.getVehicle());
        resp.setVehicleNumber(detail.getVehicleNumber());
        resp.setStatus(detail.getStatus());
        resp.setRating(detail.getRating());
        resp.setCompletedOrders(detail.getCompletedOrders());
        return resp;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }

    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }

    public String getVehicle() { return vehicle; }
    public void setVehicle(String vehicle) { this.vehicle = vehicle; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }

    public Integer getCompletedOrders() { return completedOrders; }
    public void setCompletedOrders(Integer completedOrders) { this.completedOrders = completedOrders; }
}
