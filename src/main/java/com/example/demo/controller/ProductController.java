package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品控制器 —— 用户端浏览（商家列表、分类、商品详情）
 * GET 接口公开，无需登录即可浏览
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // ==================== 商家浏览 ====================

    /**
     * 商家列表（支持搜索与按评分排序）
     * GET /api/products/merchants?keyword=中餐
     */
    @GetMapping("/merchants")
    public ResponseEntity<ApiResponse<List<MerchantDetailResponse>>> getMerchantList(
            @RequestParam(required = false) String keyword) {
        List<MerchantDetailResponse> merchants = productService.getMerchantList(keyword);
        return ResponseEntity.ok(ApiResponse.success(merchants));
    }

    /**
     * 商家详情
     * GET /api/products/merchants/{merchantId}
     */
    @GetMapping("/merchants/{merchantId}")
    public ResponseEntity<ApiResponse<MerchantDetailResponse>> getMerchantDetail(
            @PathVariable Integer merchantId) {
        MerchantDetailResponse detail = productService.getMerchantDetail(merchantId);
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    /**
     * 某商家的分类列表
     * GET /api/products/merchants/{merchantId}/categories
     */
    @GetMapping("/merchants/{merchantId}/categories")
    public ResponseEntity<ApiResponse<List<ProductCategoryResponse>>> getCategoriesByMerchant(
            @PathVariable Integer merchantId) {
        List<ProductCategoryResponse> categories = productService.getCategoriesByMerchant(merchantId);
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    /**
     * 某商家的商品列表（默认只看上架商品，支持按分类筛选）
     * GET /api/products/merchants/{merchantId}/products?categoryId=1&showAll=false
     */
    @GetMapping("/merchants/{merchantId}/products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProductsByMerchant(
            @PathVariable Integer merchantId,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Boolean showAll) {
        List<ProductResponse> products = productService.getProductsByMerchant(merchantId, categoryId, showAll);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    // ==================== 商品浏览 ====================

    /**
     * 商品详情
     * GET /api/products/{productId}
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductDetail(
            @PathVariable Integer productId) {
        ProductResponse product = productService.getProductDetail(productId);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    /**
     * 搜索商品（跨商家）
     * GET /api/products/search?keyword=辣椒
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchProducts(
            @RequestParam String keyword) {
        List<ProductResponse> products = productService.searchProducts(keyword);
        return ResponseEntity.ok(ApiResponse.success(products));
    }
}
