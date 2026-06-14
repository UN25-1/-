package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.service.AdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员控制器 —— 用户管理 / 商家审核 / 订单监控 / 骑手审核 / 数据统计
 *
 * 路径：/api/admin/**  （仅 ADMIN 角色可访问）
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // ==================== 数据统计看板 ====================

    /**
     * 管理员首页统计看板
     * GET /api/admin/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats() {
        AdminStatsResponse stats = adminService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // ==================== 用户管理 ====================

    /**
     * 所有用户列表（支持关键词搜索 + 状态筛选 + 分页）
     * GET /api/admin/users?keyword=&status=2&page=0&size=20
     * status: 不传=全部, 0=禁用, 1=启用, 2=待审核
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<UserProfileResponse> userPage = adminService.getAllUsers(keyword, status, pageRequest);
        Map<String, Object> result = Map.of(
                "content", userPage.getContent(),
                "totalElements", userPage.getTotalElements(),
                "totalPages", userPage.getTotalPages(),
                "currentPage", userPage.getNumber(),
                "size", userPage.getSize()
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 更新用户状态
     * PUT /api/admin/users/{id}/status
     * Body: {"status": 0}  0=禁用, 1=启用, 2=待审核
     */
    @PutMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "缺少 status 参数（0=禁用, 1=启用, 2=待审核）"));
        }
        adminService.updateUserStatus(id, status);
        String statusName = status == 0 ? "禁用" : status == 1 ? "启用" : "待审核";
        return ResponseEntity.ok(ApiResponse.success("用户状态已更新为：" + statusName, null));
    }

    /**
     * 查看用户详情
     * GET /api/admin/users/{id}
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserDetail(@PathVariable Integer id) {
        UserProfileResponse user = adminService.getUserDetail(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    // ==================== 商家审核 ====================

    /**
     * 商家列表（支持搜索 + enabled筛选 + 分页）
     * GET /api/admin/merchants?keyword=&enabled=true&page=0&size=20
     */
    @GetMapping("/merchants")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMerchants(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<MerchantDetailResponse> merchantPage = adminService.getMerchantList(keyword, enabled, pageRequest);
        Map<String, Object> result = Map.of(
                "content", merchantPage.getContent(),
                "totalElements", merchantPage.getTotalElements(),
                "totalPages", merchantPage.getTotalPages(),
                "currentPage", merchantPage.getNumber(),
                "size", merchantPage.getSize()
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 审核商家（启用/禁用）
     * PUT /api/admin/merchants/{id}/status
     */
    @PutMapping("/merchants/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateMerchantStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, Boolean> body) {
        Boolean enabled = body.get("enabled");
        if (enabled == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "缺少 enabled 参数"));
        }
        adminService.updateMerchantStatus(id, enabled);
        return ResponseEntity.ok(ApiResponse.success(
                enabled ? "商家已启用" : "商家已禁用", null));
    }

    // ==================== 全平台订单监控 ====================

    /**
     * 全平台订单列表（支持按状态筛选 + 分页）
     * GET /api/admin/orders?status=pending&page=0&size=20
     */
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<OrderResponse> orderPage = adminService.getAllOrders(status, pageRequest);
        Map<String, Object> result = Map.of(
                "content", orderPage.getContent(),
                "totalElements", orderPage.getTotalElements(),
                "totalPages", orderPage.getTotalPages(),
                "currentPage", orderPage.getNumber(),
                "size", orderPage.getSize()
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ==================== 骑手审核 ====================

    /**
     * 骑手列表（支持搜索 + enabled筛选 + 分页）
     * GET /api/admin/riders?keyword=&enabled=true&page=0&size=20
     */
    @GetMapping("/riders")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRiders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<RiderDetailResponse> riderPage = adminService.getRiderList(keyword, enabled, pageRequest);
        Map<String, Object> result = Map.of(
                "content", riderPage.getContent(),
                "totalElements", riderPage.getTotalElements(),
                "totalPages", riderPage.getTotalPages(),
                "currentPage", riderPage.getNumber(),
                "size", riderPage.getSize()
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 审核骑手（启用/禁用）
     * PUT /api/admin/riders/{id}/status
     */
    @PutMapping("/riders/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateRiderStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, Boolean> body) {
        Boolean enabled = body.get("enabled");
        if (enabled == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "缺少 enabled 参数"));
        }
        adminService.updateRiderStatus(id, enabled);
        return ResponseEntity.ok(ApiResponse.success(
                enabled ? "骑手已启用" : "骑手已禁用", null));
    }
}
