package com.example.demo.repository;

import com.example.demo.entity.RiderReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 骑手评价数据访问层 —— 映射 rider_reviews 表
 */
@Repository
public interface RiderReviewRepository extends JpaRepository<RiderReview, Integer> {

    /**
     * 根据订单ID和用户ID查询评价（唯一约束校验）
     */
    Optional<RiderReview> findByOrderIdAndUserId(Integer orderId, Integer userId);

    /**
     * 查询指定骑手的所有评价（分页，按创建时间倒序）
     */
    Page<RiderReview> findByRiderIdOrderByCreatedAtDesc(Integer riderId, Pageable pageable);

    /**
     * 查询指定骑手的评价总数
     */
    long countByRiderId(Integer riderId);

    /**
     * 查询指定骑手的全部评价（用于计算平均分）
     */
    List<RiderReview> findAllByRiderId(Integer riderId);

    /**
     * 查询当前用户的所有骑手评价（分页，按创建时间倒序）
     */
    Page<RiderReview> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);
}
