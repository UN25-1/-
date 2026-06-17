package com.example.demo.dto;

import java.math.BigDecimal;

/**
 * 骑手收入响应 DTO
 * <p>
 * 收入来源：已完成/已送达订单的配送费（deliveryFee）
 */
public class RiderEarningsResponse {

    /** 累计总收入（元） */
    private BigDecimal totalEarnings;

    /** 今日收入（元） */
    private BigDecimal todayEarnings;

    /** 累计完成订单数 */
    private long completedOrders;

    public BigDecimal getTotalEarnings() {
        return totalEarnings;
    }

    public void setTotalEarnings(BigDecimal totalEarnings) {
        this.totalEarnings = totalEarnings;
    }

    public BigDecimal getTodayEarnings() {
        return todayEarnings;
    }

    public void setTodayEarnings(BigDecimal todayEarnings) {
        this.todayEarnings = todayEarnings;
    }

    public long getCompletedOrders() {
        return completedOrders;
    }

    public void setCompletedOrders(long completedOrders) {
        this.completedOrders = completedOrders;
    }
}
