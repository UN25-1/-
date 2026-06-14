package com.example.demo.repository;

import com.example.demo.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户地址数据访问层 —— 所有查询均使用参数化绑定，防止SQL注入
 */
@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Integer> {

    /** 查询用户所有地址（按默认地址优先 + 创建时间排序） */
    List<UserAddress> findByUserIdOrderByIsDefaultDescCreatedAtDesc(Integer userId);

    /** 查询用户默认地址 */
    Optional<UserAddress> findByUserIdAndIsDefault(Integer userId, Boolean isDefault);

    /** 统计用户地址数量 */
    long countByUserId(Integer userId);

    /** 将用户所有地址设为非默认 */
    @Modifying
    @Query("UPDATE UserAddress a SET a.isDefault = false WHERE a.userId = :userId")
    void clearDefaultByUserId(@Param("userId") Integer userId);

    /** 检查地址是否属于指定用户（安全校验） */
    @Query("SELECT COUNT(a) > 0 FROM UserAddress a WHERE a.id = :id AND a.userId = :userId")
    boolean existsByIdAndUserId(@Param("id") Integer id, @Param("userId") Integer userId);
}
