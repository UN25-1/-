package com.example.demo.repository;

import com.example.demo.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 商品数据访问层
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    /** 查询某商家的所有商品（按创建时间降序） */
    List<Product> findByMerchantIdOrderByCreatedAtDesc(Integer merchantId);

    /** 查询某商家某个分类下的商品 */
    List<Product> findByMerchantIdAndCategoryId(Integer merchantId, Integer categoryId);

    /** 查询某商家所有上架商品 */
    List<Product> findByMerchantIdAndIsAvailableTrue(Integer merchantId);

    /** 查询某商家某分类下所有上架商品 */
    List<Product> findByMerchantIdAndCategoryIdAndIsAvailableTrue(Integer merchantId, Integer categoryId);

    /** 检查商品是否属于指定商家（安全校验） */
    boolean existsByIdAndMerchantId(Integer id, Integer merchantId);

    /** 根据ID和商家ID查找商品 */
    Optional<Product> findByIdAndMerchantId(Integer id, Integer merchantId);

    /** 按名称搜索商品（跨商家） */
    List<Product> findByNameContainingAndIsAvailableTrue(String name);

    /**
     * 原子扣减库存（并发安全）
     * WHERE stock >= quantity 保证不会超卖，受影响行数 = 0 时表示库存不足
     */
    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.stock = p.stock - :quantity WHERE p.id = :productId AND p.stock >= :quantity")
    int decrementStock(@Param("productId") Integer productId, @Param("quantity") Integer quantity);

    /**
     * 原子回补库存
     */
    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.stock = p.stock + :quantity WHERE p.id = :productId")
    int incrementStock(@Param("productId") Integer productId, @Param("quantity") Integer quantity);
}
