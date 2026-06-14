package com.example.demo.repository;

import com.example.demo.entity.MerchantDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 商家详情数据访问层 —— 所有查询均使用参数化绑定，防止SQL注入
 */
@Repository
public interface MerchantDetailRepository extends JpaRepository<MerchantDetail, Integer> {

    /** 根据用户ID查询商家详情（一个用户只能有一个店铺） */
    Optional<MerchantDetail> findByUserId(Integer userId);

    /** 判断用户是否已创建商家 */
    boolean existsByUserId(Integer userId);

    /** 查询所有商家（按评分降序） */
    List<MerchantDetail> findAllByOrderByRatingDesc();

    /** 按店铺名称模糊搜索 */
    List<MerchantDetail> findByShopNameContaining(String shopName);
}
