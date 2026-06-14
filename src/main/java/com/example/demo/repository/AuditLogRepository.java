package com.example.demo.repository;

import com.example.demo.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 审计日志数据访问层
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /** 按目标类型和目标ID查询操作记录（按时间降序） */
    List<AuditLog> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, Integer targetId);

    /** 按操作人查询 */
    List<AuditLog> findByOperatorIdOrderByCreatedAtDesc(Integer operatorId);
}
