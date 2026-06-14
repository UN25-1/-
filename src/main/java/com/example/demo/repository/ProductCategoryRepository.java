package com.example.demo.repository;

import com.example.demo.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 菜品分类数据访问层
 */
@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Integer> {

    /** 查询某商家的所有分类（按排序权重升序） */
    List<ProductCategory> findByMerchantIdOrderBySortOrderAsc(Integer merchantId);

    /** 查询某商家所有上架分类（用户端浏览用） */
    List<ProductCategory> findByMerchantIdAndIsAvailableTrueOrderBySortOrderAsc(Integer merchantId);

    /** 检查某商家下是否存在同名分类 */
    boolean existsByMerchantIdAndName(Integer merchantId, String name);

    /** 检查分类是否属于指定商家（安全校验） */
    boolean existsByIdAndMerchantId(Integer id, Integer merchantId);

    /** 根据ID和商家ID查找分类 */
    Optional<ProductCategory> findByIdAndMerchantId(Integer id, Integer merchantId);
}
