package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.QualificationResponse;
import com.example.demo.service.FileUploadService;
import com.example.demo.service.QualificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/qualification")
public class QualificationController {

    private final QualificationService qualificationService;
    private final FileUploadService fileUploadService;

    public QualificationController(QualificationService qualificationService, FileUploadService fileUploadService) {
        this.qualificationService = qualificationService;
        this.fileUploadService = fileUploadService;
    }

    private Integer getCurrentUserId() {
        return (Integer) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // ========== 商家/骑手：上传证件 ==========

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Void>> uploadDocument(
            @RequestParam String role,
            @RequestParam String docType,
            @RequestParam MultipartFile file) throws IOException {
        Integer userId = getCurrentUserId();
        // 真实保存文件到磁盘
        String url = fileUploadService.uploadImage(file).getUrl();
        qualificationService.uploadDocument(userId, role, docType, url);
        return ResponseEntity.ok(ApiResponse.<Void>success("上传成功", null));
    }

    // ========== 商家/骑手：提交审核（全部上传后触发） ==========

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<Void>> submitForReview(@RequestBody Map<String, String> body) {
        Integer userId = getCurrentUserId();
        String role = body.getOrDefault("role", "merchant");
        qualificationService.submitForReview(userId, role);
        return ResponseEntity.ok(ApiResponse.<Void>success("已提交审核", null));
    }

    // ========== 商家/骑手：查看自己的审核状态 ==========

    @GetMapping("/my-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyStatus() {
        Integer userId = getCurrentUserId();
        String status = qualificationService.getUserReviewStatus(userId);
        List<QualificationResponse> docs = qualificationService.getUserDocuments(userId);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "reviewStatus", status != null ? status : "none",
                "documents", docs
        )));
    }

    // ========== 管理员：审核列表 ==========

    @GetMapping("/admin/pending")
    public ResponseEntity<ApiResponse<List<QualificationResponse>>> getPendingApplications() {
        return ResponseEntity.ok(ApiResponse.success(qualificationService.getPendingApplications()));
    }

    @PostMapping("/admin/approve/{userId}")
    public ResponseEntity<ApiResponse<Void>> approve(@PathVariable Integer userId) {
        qualificationService.approve(userId);
        return ResponseEntity.ok(ApiResponse.<Void>success("审核已通过", null));
    }

    @PostMapping("/admin/reject/{userId}")
    public ResponseEntity<ApiResponse<Void>> reject(@PathVariable Integer userId,
                                                     @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", "") : "";
        qualificationService.reject(userId, reason);
        return ResponseEntity.ok(ApiResponse.<Void>success("已驳回", null));
    }
}
