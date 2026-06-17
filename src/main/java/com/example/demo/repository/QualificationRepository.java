package com.example.demo.repository;

import com.example.demo.entity.Qualification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QualificationRepository extends JpaRepository<Qualification, Integer> {

    List<Qualification> findByUserIdAndUserRole(Integer userId, String userRole);

    @Query("SELECT q.userId FROM Qualification q WHERE q.userRole = :role GROUP BY q.userId ORDER BY MAX(q.createdAt) DESC")
    List<Integer> findDistinctUserIdsByRole(@org.springframework.data.repository.query.Param("role") String role);

    List<Qualification> findByUserId(Integer userId);

    List<Qualification> findByUserIdAndReviewStatus(Integer userId, String reviewStatus);

    @Query("SELECT DISTINCT q.userId FROM Qualification q WHERE q.userRole = :role AND q.reviewStatus = 'pending'")
    List<Integer> findPendingUserIdsByRole(@org.springframework.data.repository.query.Param("role") String role);
}
