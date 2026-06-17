package com.example.demo.service;

import com.example.demo.dto.QualificationResponse;
import com.example.demo.entity.*;
import com.example.demo.exception.BusinessException;
import com.example.demo.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QualificationService {

    private static final Logger log = LoggerFactory.getLogger(QualificationService.class);

    private final QualificationRepository qRepo;
    private final UserRepository userRepo;
    private final MerchantDetailRepository merchantRepo;
    private final RiderDetailRepository riderRepo;

    public QualificationService(QualificationRepository qRepo, UserRepository userRepo,
                                MerchantDetailRepository merchantRepo, RiderDetailRepository riderRepo) {
        this.qRepo = qRepo;
        this.userRepo = userRepo;
        this.merchantRepo = merchantRepo;
        this.riderRepo = riderRepo;
    }

    // ========== 申请人上传文件（状态：uploaded，尚未提交审核） ==========

    @Transactional
    public void uploadDocument(Integer userId, String userRole, String docType, String docUrl) {
        qRepo.findByUserIdAndUserRole(userId, userRole).stream()
                .filter(q -> q.getDocType().equals(docType))
                .forEach(q -> qRepo.delete(q));

        Qualification q = new Qualification(userId, userRole, docType, docUrl);
        q.setReviewStatus("uploaded"); // 仅上传，未提交审核
        qRepo.save(q);
        log.info("上传资质文件：userId={}, role={}, type={}", userId, userRole, docType);
    }

    // ========== 提交审核 ==========

    @Transactional
    public void submitForReview(Integer userId, String userRole) {
        List<Qualification> docs = qRepo.findByUserIdAndUserRole(userId, userRole);
        // 检查是否所有必需类型都已上传
        Set<String> uploaded = docs.stream().map(Qualification::getDocType).collect(Collectors.toSet());
        List<String> required = "merchant".equals(userRole)
                ? List.of("business_license", "id_card", "health_cert")
                : List.of("id_card", "health_cert");
        for (String type : required) {
            if (!uploaded.contains(type)) {
                throw new BusinessException(400, "请先上传所有必需的证件后再提交审核（缺少：" + type + "）");
            }
        }
        // 所有 uploaded 状态文档改为 pending
        for (Qualification q : docs) {
            if ("uploaded".equals(q.getReviewStatus())) {
                q.setReviewStatus("pending");
                qRepo.save(q);
            }
        }
        log.info("提交审核：userId={}, role={}, docs={}", userId, userRole, docs.size());
    }

    // ========== 获取用户状态 ==========

    /** 返回用户审核状态：null=未上传, uploading=已上传未提交, pending=审核中, approved=通过, rejected=驳回 */
    public String getUserReviewStatus(Integer userId) {
        List<Qualification> docs = qRepo.findByUserId(userId);
        if (docs.isEmpty()) return null;
        boolean hasPending = docs.stream().anyMatch(q -> "pending".equals(q.getReviewStatus()));
        if (hasPending) return "pending";
        boolean hasUploaded = docs.stream().anyMatch(q -> "uploaded".equals(q.getReviewStatus()));
        if (hasUploaded) return "uploading";
        boolean allApproved = docs.stream().allMatch(q -> "approved".equals(q.getReviewStatus()));
        if (allApproved) return "approved";
        return "rejected";
    }

    /** 获取用户已上传的资质文件 */
    public List<QualificationResponse> getUserDocuments(Integer userId) {
        return qRepo.findByUserId(userId).stream()
                .map(QualificationResponse::from).collect(Collectors.toList());
    }

    // ========== 管理员审核列表（按申请人分组） ==========

    public List<QualificationResponse> getPendingApplications() {
        Map<Integer, List<Qualification>> grouped = new LinkedHashMap<>();

        // 查询所有 pending 状态的文档
        List<Qualification> all = qRepo.findAll();
        all.stream()
                .filter(q -> "pending".equals(q.getReviewStatus()))
                .forEach(q -> grouped.computeIfAbsent(q.getUserId(), k -> new ArrayList<>()).add(q));

        List<QualificationResponse> result = new ArrayList<>();
        for (Map.Entry<Integer, List<Qualification>> entry : grouped.entrySet()) {
            Integer userId = entry.getKey();
            List<Qualification> docs = entry.getValue();

            QualificationResponse summary = new QualificationResponse();
            summary.setUserId(userId);
            summary.setUserRole(docs.get(0).getUserRole());
            // 找最早提交时间
            LocalDateTime earliest = docs.stream().map(Qualification::getCreatedAt)
                    .min(Comparator.naturalOrder()).orElse(null);
            summary.setCreatedAt(earliest);
            summary.setDocuments(docs.stream().map(QualificationResponse::from).collect(Collectors.toList()));

            // 获取用户名
            userRepo.findById(userId).ifPresent(u -> summary.setUsername(u.getUsername()));

            result.add(summary);
        }
        return result;
    }

    // ========== 管理员审核操作 ==========

    @Transactional
    public void approve(Integer userId) {
        List<Qualification> docs = qRepo.findByUserIdAndReviewStatus(userId, "pending");
        if (docs.isEmpty()) throw new BusinessException(400, "该用户没有待审核的申请");

        for (Qualification q : docs) {
            q.setReviewStatus("approved");
            q.setReviewedAt(LocalDateTime.now());
            qRepo.save(q);
        }

        // 启用商家/骑手账号
        String role = docs.get(0).getUserRole();
        if ("merchant".equals(role)) {
            enableMerchant(userId);
        } else if ("rider".equals(role)) {
            enableRider(userId);
        }

        // 将用户状态从"待审核(2)"改为"正常启用(1)"
        userRepo.findById(userId).ifPresent(user -> {
            user.setStatus(1);
            userRepo.save(user);
        });

        log.info("审核通过：userId={}, role={}", userId, role);
    }

    @Transactional
    public void reject(Integer userId, String reason) {
        List<Qualification> docs = qRepo.findByUserIdAndReviewStatus(userId, "pending");
        if (docs.isEmpty()) throw new BusinessException(400, "该用户没有待审核的申请");

        for (Qualification q : docs) {
            q.setReviewStatus("rejected");
            q.setReviewRemark(reason);
            q.setReviewedAt(LocalDateTime.now());
            qRepo.save(q);
        }

        log.info("审核驳回：userId={}, reason={}", userId, reason);
    }

    private void enableMerchant(Integer userId) {
        merchantRepo.findByUserId(userId).ifPresent(m -> {
            m.setEnabled(true);
            merchantRepo.save(m);
        });
    }

    private void enableRider(Integer userId) {
        riderRepo.findByUserId(userId).ifPresent(r -> {
            r.setEnabled(true);
            riderRepo.save(r);
        });
    }
}
