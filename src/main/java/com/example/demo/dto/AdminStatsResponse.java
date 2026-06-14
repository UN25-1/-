package com.example.demo.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 管理员统计看板 响应DTO
 */
public class AdminStatsResponse {

    /** 总用户数 */
    private long totalUsers;
    /** 总商家数 */
    private long totalMerchants;
    /** 总骑手数 */
    private long totalRiders;

    /** 今日订单量 */
    private long todayOrderCount;
    /** 今日 GMV */
    private BigDecimal todayGMV;
    /** 待处理订单数 */
    private long pendingOrderCount;

    /** 各状态订单数量分布 */
    private Map<String, Long> orderStatusDistribution;

    /** 热门商家排行（Top N） */
    private java.util.List<TopMerchant> topMerchants;

    /** 热门商家榜单项 */
    public static class TopMerchant {
        private Integer merchantId;
        private String shopName;
        private long orderCount;
        private BigDecimal totalRevenue;

        public static TopMerchant of(Integer merchantId, String shopName, long orderCount, BigDecimal totalRevenue) {
            TopMerchant m = new TopMerchant();
            m.merchantId = merchantId;
            m.shopName = shopName;
            m.orderCount = orderCount;
            m.totalRevenue = totalRevenue;
            return m;
        }

        public Integer getMerchantId() { return merchantId; }
        public void setMerchantId(Integer merchantId) { this.merchantId = merchantId; }
        public String getShopName() { return shopName; }
        public void setShopName(String shopName) { this.shopName = shopName; }
        public long getOrderCount() { return orderCount; }
        public void setOrderCount(long orderCount) { this.orderCount = orderCount; }
        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    }

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }
    public long getTotalMerchants() { return totalMerchants; }
    public void setTotalMerchants(long totalMerchants) { this.totalMerchants = totalMerchants; }
    public long getTotalRiders() { return totalRiders; }
    public void setTotalRiders(long totalRiders) { this.totalRiders = totalRiders; }
    public long getTodayOrderCount() { return todayOrderCount; }
    public void setTodayOrderCount(long todayOrderCount) { this.todayOrderCount = todayOrderCount; }
    public BigDecimal getTodayGMV() { return todayGMV; }
    public void setTodayGMV(BigDecimal todayGMV) { this.todayGMV = todayGMV; }
    public long getPendingOrderCount() { return pendingOrderCount; }
    public void setPendingOrderCount(long pendingOrderCount) { this.pendingOrderCount = pendingOrderCount; }
    public Map<String, Long> getOrderStatusDistribution() { return orderStatusDistribution; }
    public void setOrderStatusDistribution(Map<String, Long> orderStatusDistribution) { this.orderStatusDistribution = orderStatusDistribution; }
    public java.util.List<TopMerchant> getTopMerchants() { return topMerchants; }
    public void setTopMerchants(java.util.List<TopMerchant> topMerchants) { this.topMerchants = topMerchants; }
}
