package com.example.demo.repository;

import com.example.demo.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 订单数据访问层 —— 所有查询均使用参数化绑定，防止SQL注入
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    /** 查询某用户的所有订单（按创建时间倒序） */
    List<Order> findByUserIdOrderByCreatedAtDesc(Integer userId);

    /** 分页查询某用户的订单 */
    Page<Order> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    /** 查询某用户指定状态的订单 */
    List<Order> findByUserIdAndOrderStatusOrderByCreatedAtDesc(Integer userId, String orderStatus);

    /** 分页查询某用户指定状态的订单 */
    Page<Order> findByUserIdAndOrderStatusOrderByCreatedAtDesc(Integer userId, String orderStatus, Pageable pageable);

    /** 查询某商家的所有订单（按创建时间倒序） */
    List<Order> findByMerchantIdOrderByCreatedAtDesc(Integer merchantId);

    /** 分页查询某商家的订单 */
    Page<Order> findByMerchantIdOrderByCreatedAtDesc(Integer merchantId, Pageable pageable);

    /** 查询某商家指定状态的订单 */
    List<Order> findByMerchantIdAndOrderStatusOrderByCreatedAtDesc(Integer merchantId, String orderStatus);

    /** 分页查询某商家指定状态的订单 */
    Page<Order> findByMerchantIdAndOrderStatusOrderByCreatedAtDesc(Integer merchantId, String orderStatus, Pageable pageable);

    /** 查询某骑手的配送订单 */
    List<Order> findByRiderIdOrderByCreatedAtDesc(Integer riderId);

    /** 分页查询某骑手的配送订单 */
    Page<Order> findByRiderIdOrderByCreatedAtDesc(Integer riderId, Pageable pageable);

    /** 分页查询某骑手指定状态的订单 */
    Page<Order> findByRiderIdAndOrderStatusOrderByCreatedAtDesc(Integer riderId, String orderStatus, Pageable pageable);

    /** 查询指定状态且尚未分配骑手的订单（按创建时间升序，用于骑手抢单池） */
    List<Order> findByOrderStatusInAndRiderIdIsNullOrderByCreatedAtAsc(Collection<String> orderStatuses);

    /** 检查订单是否属于指定用户（安全校验） */
    boolean existsByIdAndUserId(Integer id, Integer userId);

    /** 检查订单是否属于指定商家（安全校验） */
    boolean existsByIdAndMerchantId(Integer id, Integer merchantId);

    /** 查询超时未支付的订单（用于定时自动取消） */
    List<Order> findByOrderStatusAndCreatedAtBefore(String orderStatus, LocalDateTime createdAtBefore);

    /** 查询指定状态集合且更新时间早于阈值的订单（用于骑手接单超时释放） */
    List<Order> findByOrderStatusInAndUpdatedAtBefore(Collection<String> orderStatuses, LocalDateTime updatedAtBefore);

    /** 管理员：按状态分页查询所有订单 */
    Page<Order> findByOrderStatusOrderByCreatedAtDesc(String orderStatus, Pageable pageable);

    /** 管理员：分页查询所有订单 */
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** 查询指定时间范围内的订单 */
    List<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /** 统计指定状态集合的订单数量 */
    long countByOrderStatusIn(Collection<String> orderStatuses);

    // ==================== 聚合统计查询（替代内存全表遍历） ====================

    /**
     * 按订单状态分组统计数量（数据库层聚合，避免全表加载）
     * @return Map<orderStatus, count>
     */
    @Query("SELECT o.orderStatus AS status, COUNT(o) AS cnt FROM Order o GROUP BY o.orderStatus")
    List<Object[]> countGroupByOrderStatus();

    /**
     * 查询指定时间范围内的订单总金额（GMV）
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.createdAt BETWEEN :start AND :end")
    java.math.BigDecimal sumTotalAmountBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 按商家统计订单数量 Top N（数据库层排序）
     */
    @Query("SELECT o.merchantId, COUNT(o) FROM Order o GROUP BY o.merchantId ORDER BY COUNT(o) DESC")
    List<Object[]> countByMerchantIdGrouped();

    /**
     * 按商家统计营收总额
     */
    @Query("SELECT o.merchantId, SUM(o.totalAmount) FROM Order o GROUP BY o.merchantId")
    List<Object[]> sumAmountByMerchantIdGrouped();
}
