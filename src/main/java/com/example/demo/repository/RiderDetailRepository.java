package com.example.demo.repository;

import com.example.demo.entity.RiderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 骑手详情数据访问层
 */
@Repository
public interface RiderDetailRepository extends JpaRepository<RiderDetail, Integer> {

    /** 通过 userId 查询骑手档案 */
    Optional<RiderDetail> findByUserId(Integer userId);
}
