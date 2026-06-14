package com.example.demo.repository;

import com.example.demo.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
