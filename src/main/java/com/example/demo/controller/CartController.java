package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.CartItemRequest;
import com.example.demo.dto.CartItemResponse;
import com.example.demo.dto.CartMerchantGroup;
import com.example.demo.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 购物车控制器 —— 购物车CRUD + 跨商家校验
 * 所有接口均需携带 JWT Token 认证（仅 user 角色操作购物车）
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
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

    // ==================== 购物车CRUD ====================

    /**
     * 查询我的购物车（按商家分组展示）
     * GET /api/cart
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CartMerchantGroup>>> getMyCart() {
        List<CartMerchantGroup> cart = cartService.getMyCart(getCurrentUserId());
        if (cart.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("购物车为空", cart));
        }
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    /**
     * 添加商品到购物车（RequestBody 方式）
     * POST /api/cart
     * body: {"productId": 1, "quantity": 2}
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CartItemResponse>> addToCart(
            @Valid @RequestBody CartItemRequest request) {
        if (request.getProductId() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "商品ID不能为空"));
        }
        CartItemResponse item = cartService.addToCart(getCurrentUserId(), request);
        return buildAddToCartResponse(item);
    }

    /**
     * 添加商品到购物车（URL 路径方式，推荐）
     * POST /api/cart/{productId}
     * body（可选）: {"quantity": 2}  —— 不传默认数量=1
     *
     * 重复添加同一商品时自动累加数量，不会报错
     */
    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse<CartItemResponse>> addToCartByPath(
            @PathVariable Integer productId,
            @Valid @RequestBody(required = false) CartItemRequest request) {
        int quantity = (request != null && request.getQuantity() != null) ? request.getQuantity() : 1;
        CartItemResponse item = cartService.addToCart(getCurrentUserId(), productId, quantity);
        return buildAddToCartResponse(item);
    }

    /**
     * 根据 isNewItem 标志返回不同的成功消息
     */
    private ResponseEntity<ApiResponse<CartItemResponse>> buildAddToCartResponse(CartItemResponse item) {
        if (Boolean.TRUE.equals(item.getIsNewItem())) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("已添加到购物车", item));
        } else {
            return ResponseEntity.ok(ApiResponse.success("购物车数量已更新", item));
        }
    }

    /**
     * 修改购物车商品数量
     * PUT /api/cart/{productId}
     */
    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponse<CartItemResponse>> updateQuantity(
            @PathVariable Integer productId,
            @Valid @RequestBody CartItemRequest request) {
        CartItemResponse item = cartService.updateQuantity(getCurrentUserId(), productId, request.getQuantity());
        return ResponseEntity.ok(ApiResponse.success("数量已更新", item));
    }

    /**
     * 删除购物车中的指定商品
     * DELETE /api/cart/{productId}
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> removeFromCart(@PathVariable Integer productId) {
        cartService.removeFromCart(getCurrentUserId(), productId);
        return ResponseEntity.ok(ApiResponse.success("已从购物车移除", null));
    }

    /**
     * 清空整个购物车
     * DELETE /api/cart
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearAllCart() {
        cartService.clearAllCart(getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("购物车已清空", null));
    }

    // ==================== 跨商家校验 ====================

    /**
     * 结算前校验：检查购物车中是否包含多个商家的商品
     * GET /api/cart/check
     */
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkCrossMerchant() {
        Map<String, Object> result = cartService.checkCrossMerchant(getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
