package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.entity.UserAddress;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 用户控制器 —— 个人信息管理 + 地址管理
 * 所有接口均需携带 JWT Token 认证
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 从SecurityContext获取当前登录用户ID
     */
    private Integer getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Integer) {
            return (Integer) principal;
        }
        throw new RuntimeException("无法获取当前用户信息");
    }

    // ==================== 个人信息 ====================

    /**
     * 获取当前用户个人信息
     * GET /api/user/profile
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile() {
        Map<String, Object> profile = userService.getProfile(getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    /**
     * 更新个人信息（手机号）
     * PUT /api/user/profile
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProfile(
            @Valid @RequestBody UpdateUserRequest request) {
        Map<String, Object> profile = userService.updateProfile(getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("个人信息更新成功", profile));
    }

    /**
     * 修改密码
     * PUT /api/user/password
     */
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("密码修改成功，请重新登录", null));
    }

    /**
     * 上传头像
     * POST /api/user/avatar
     */
    @PostMapping("/avatar")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAvatar(
            @RequestParam("file") MultipartFile file) {
        String url = userService.saveAvatar(getCurrentUserId(), file);
        Map<String, String> result = Map.of("url", url);
        return ResponseEntity.ok(ApiResponse.success("头像上传成功", result));
    }

    // ==================== 账户管理 ====================

    /**
     * 删除账户（软删除，需要JWT认证）
     * DELETE /api/user/account
     */
    @DeleteMapping("/account")
    public ResponseEntity<ApiResponse<Void>> deleteAccount() {
        userService.deleteAccount(getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("账户已删除", null));
    }

    // ==================== 地址管理 ====================

    /**
     * 获取当前用户所有地址
     * GET /api/user/addresses
     */
    @GetMapping("/addresses")
    public ResponseEntity<ApiResponse<List<UserAddress>>> getAddresses() {
        List<UserAddress> addresses = userService.getAddresses(getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(addresses));
    }

    /**
     * 新增地址
     * POST /api/user/addresses
     */
    @PostMapping("/addresses")
    public ResponseEntity<ApiResponse<UserAddress>> addAddress(
            @Valid @RequestBody AddressRequest request) {
        UserAddress address = userService.addAddress(getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("地址添加成功", address));
    }

    /**
     * 编辑地址
     * PUT /api/user/addresses/{addressId}
     */
    @PutMapping("/addresses/{addressId}")
    public ResponseEntity<ApiResponse<UserAddress>> updateAddress(
            @PathVariable Integer addressId,
            @Valid @RequestBody AddressRequest request) {
        UserAddress address = userService.updateAddress(getCurrentUserId(), addressId, request);
        return ResponseEntity.ok(ApiResponse.success("地址更新成功", address));
    }

    /**
     * 删除地址
     * DELETE /api/user/addresses/{addressId}
     */
    @DeleteMapping("/addresses/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(@PathVariable Integer addressId) {
        userService.deleteAddress(getCurrentUserId(), addressId);
        return ResponseEntity.ok(ApiResponse.success("地址删除成功", null));
    }

    /**
     * 批量删除地址
     * DELETE /api/user/addresses/batch
     * Body: {"ids": [1, 2, 3]}
     */
    @DeleteMapping("/addresses/batch")
    public ResponseEntity<ApiResponse<Void>> deleteAddressesBatch(
            @RequestBody Map<String, List<Integer>> body) {
        List<Integer> ids = body.get("ids");
        userService.deleteAddressesBatch(getCurrentUserId(), ids);
        return ResponseEntity.ok(ApiResponse.success("已删除 " + ids.size() + " 个地址", null));
    }

    /**
     * 设置默认地址
     * PUT /api/user/addresses/{addressId}/default
     */
    @PutMapping("/addresses/{addressId}/default")
    public ResponseEntity<ApiResponse<Void>> setDefaultAddress(@PathVariable Integer addressId) {
        userService.setDefaultAddress(getCurrentUserId(), addressId);
        return ResponseEntity.ok(ApiResponse.success("默认地址设置成功", null));
    }
}
