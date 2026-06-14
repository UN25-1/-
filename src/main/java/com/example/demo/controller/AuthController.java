package com.example.demo.controller;

import com.example.demo.config.JwtUtils;
import com.example.demo.config.TokenBlacklist;
import com.example.demo.dto.*;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器 —— 注册 / 登录 / Token刷新 / 登出
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final TokenBlacklist tokenBlacklist;

    public AuthController(UserService userService,
                          JwtUtils jwtUtils,
                          TokenBlacklist tokenBlacklist) {
        this.userService = userService;
        this.jwtUtils = jwtUtils;
        this.tokenBlacklist = tokenBlacklist;
    }

    /**
     * 用户注册
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(
            @Valid @RequestBody RegisterRequest request) {
        Map<String, Object> userData = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("注册成功", userData));
    }

    /**
     * 用户登录（返回JWT Token）
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @Valid @RequestBody LoginRequest request) {
        Map<String, Object> userData = userService.login(request);

        // 生成JWT Token
        Integer userId = (Integer) userData.get("id");
        String username = (String) userData.get("username");
        String role = (String) userData.get("role");

        String accessToken = jwtUtils.generateAccessToken(userId, username, role);
        String refreshToken = jwtUtils.generateRefreshToken(userId, username);

        // 将Token加入返回数据
        Map<String, Object> result = new HashMap<>();
        result.put("user", userData);
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);

        return ResponseEntity.ok(ApiResponse.success("登录成功", result));
    }

    /**
     * 刷新Token
     * POST /api/auth/refresh
     * 使用Refresh Token换取新的Access Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshToken(
            HttpServletRequest request) {
        String refreshToken = extractBearerToken(request);

        if (refreshToken == null || !jwtUtils.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(401, "Refresh Token无效或已过期"));
        }

        // 检查是否在黑名单中
        if (tokenBlacklist.isBlacklisted(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(401, "Refresh Token已失效"));
        }

        // 校验Token类型：仅允许 Refresh Token 刷新
        String tokenType = jwtUtils.parseToken(refreshToken).get("type", String.class);
        if (!"refresh".equals(tokenType)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(401, "仅支持使用 Refresh Token 刷新"));
        }

        Integer userId = jwtUtils.getUserIdFromToken(refreshToken);
        String username = jwtUtils.getUsernameFromToken(refreshToken);

        // 重新查库获取最新角色（防止角色变更后Token中的角色过时）
        var userData = userService.getProfile(userId);
        String role = (String) userData.get("role");

        String newAccessToken = jwtUtils.generateAccessToken(userId, username, role);

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", newAccessToken);

        return ResponseEntity.ok(ApiResponse.success("Token刷新成功", result));
    }

    /**
     * 登出（将当前Token加入黑名单）
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String token = extractBearerToken(request);
        if (token != null) {
            tokenBlacklist.add(token);
        }
        return ResponseEntity.ok(ApiResponse.success("已登出", null));
    }

    /**
     * 从请求头提取Bearer Token
     */
    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
