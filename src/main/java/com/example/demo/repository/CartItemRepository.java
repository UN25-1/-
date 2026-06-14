package com.example.demo.repository;

import com.example.demo.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 购物车数据访问层 —— 所有查询均使用参数化绑定，防止SQL注入
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {

    /** 查询某用户的所有购物车项（按添加时间倒序） */
    List<CartItem> findByUserIdOrderByCreatedAtDesc(Integer userId);

    /** 查询某用户购物车中指定商品的记录 */
    Optional<CartItem> findByUserIdAndProductId(Integer userId, Integer productId);

    /** 检查某用户购物车中是否已有某商品 */
    boolean existsByUserIdAndProductId(Integer userId, Integer productId);

    /** 删除某用户购物车中指定商品 */
    @Modifying
    @Transactional
    void deleteByUserIdAndProductId(Integer userId, Integer productId);

    /** 查询某用户购物车中指定商品ID列表的购物车项（用于结算校验/清空） */
    @Query("SELECT c FROM CartItem c WHERE c.userId = :userId AND c.productId IN :productIds")
    List<CartItem> findByUserIdAndProductIdIn(@Param("userId") Integer userId,
                                               @Param("productIds") List<Integer> productIds);

    /** 批量删除某用户购物车中指定商品（下单成功后清空） */
    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem c WHERE c.userId = :userId AND c.productId IN :productIds")
    void deleteByUserIdAndProductIdIn(@Param("userId") Integer userId,
                                       @Param("productIds") List<Integer> productIds);

    /** 清空某用户购物车 */
    @Modifying
    @Transactional
    void deleteAllByUserId(Integer userId);

    /** 删除所有用户购物车中指定商品的记录（商品下架/删除后联动清理） */
    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem c WHERE c.productId = :productId")
    void deleteByProductId(@Param("productId") Integer productId);

    /** 统计某用户购物车商品种类数 */
    long countByUserId(Integer userId);
}
