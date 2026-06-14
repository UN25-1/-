package com.example.demo.repository;

import com.example.demo.entity.MerchantReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 商家评价数据访问层 —— 映射 merchant_reviews 表
 */
@Repository
public interface MerchantReviewRepository extends JpaRepository<MerchantReview, Integer> {

    /**
     * 根据订单ID和用户ID查询评价（唯一约束校验）
     */
    Optional<MerchantReview> findByOrderIdAndUserId(Integer orderId, Integer userId);

    /**
     * 查找属于指定商家（通过订单.merchant_id）的评价（分页，按创建时间倒序）
     */
    @Query("SELECT r FROM MerchantReview r JOIN Order o ON r.orderId = o.id WHERE o.merchantId = :merchantId ORDER BY r.createdAt DESC")
    Page<MerchantReview> findByMerchantId(@Param("merchantId") Integer merchantId, Pageable pageable);

    /**
     * 统计指定商家的评价数量
     */
    @Query("SELECT COUNT(r) FROM MerchantReview r JOIN Order o ON r.orderId = o.id WHERE o.merchantId = :merchantId")
    long countByMerchantId(@Param("merchantId") Integer merchantId);

    /**
     * 查询指定商家的所有评价（用于计算平均分）
     */
    @Query("SELECT r FROM MerchantReview r JOIN Order o ON r.orderId = o.id WHERE o.merchantId = :merchantId")
    List<MerchantReview> findAllByMerchantId(@Param("merchantId") Integer merchantId);

    /**
     * 查询当前用户的所有商家评价（分页，按创建时间倒序）
     */
    Page<MerchantReview> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);
}
