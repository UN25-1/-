package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.service.FileUploadService;
import com.example.demo.service.MerchantService;
import com.example.demo.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 商家控制器 —— 店铺信息管理 + 分类管理 + 商品管理
 * 所有接口均需 ROLE_MERCHANT 权限
 */
@RestController
@RequestMapping("/api/merchant")
public class MerchantController {

    private final MerchantService merchantService;
    private final ProductService productService;
    private final FileUploadService fileUploadService;

    public MerchantController(MerchantService merchantService,
                              ProductService productService,
                              FileUploadService fileUploadService) {
        this.merchantService = merchantService;
        this.productService = productService;
        this.fileUploadService = fileUploadService;
    }

    private Integer getCurrentUserId() {
        return (Integer) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // ==================== 店铺信息 ====================

    /**
     * 商家入驻/完善店铺信息（不存在则创建，存在则更新）
     * POST /api/merchant/detail
     */
    @PostMapping("/detail")
    public ResponseEntity<ApiResponse<MerchantDetailResponse>> createOrUpdateDetail(
            @Valid @RequestBody MerchantDetailRequest request) {
        MerchantDetailResponse detail = merchantService.createOrUpdateDetail(getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("店铺信息保存成功", detail));
    }

    /**
     * 查看自己的店铺信息
     * GET /api/merchant/detail
     */
    @GetMapping("/detail")
    public ResponseEntity<ApiResponse<MerchantDetailResponse>> getMyDetail() {
        MerchantDetailResponse detail = merchantService.getMyDetail(getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    /**
     * 编辑店铺信息
     * PUT /api/merchant/detail
     */
    @PutMapping("/detail")
    public ResponseEntity<ApiResponse<MerchantDetailResponse>> updateDetail(
            @Valid @RequestBody MerchantDetailRequest request) {
        MerchantDetailResponse detail = merchantService.updateDetail(getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("店铺信息更新成功", detail));
    }

    // ==================== 分类管理 ====================

    /**
     * 查看自己店铺的分类列表
     * GET /api/merchant/categories
     */
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<ProductCategoryResponse>>> getMyCategories() {
        List<ProductCategoryResponse> categories = productService.getMyCategories(getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    /**
     * 添加菜品分类
     * POST /api/merchant/categories
     */
    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<ProductCategoryResponse>> addCategory(
            @Valid @RequestBody ProductCategoryRequest request) {
        ProductCategoryResponse category = productService.addCategory(getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("分类添加成功", category));
    }

    /**
     * 编辑菜品分类
     * PUT /api/merchant/categories/{categoryId}
     */
    @PutMapping("/categories/{categoryId}")
    public ResponseEntity<ApiResponse<ProductCategoryResponse>> updateCategory(
            @PathVariable Integer categoryId,
            @Valid @RequestBody ProductCategoryRequest request) {
        ProductCategoryResponse category = productService.updateCategory(getCurrentUserId(), categoryId, request);
        return ResponseEntity.ok(ApiResponse.success("分类更新成功", category));
    }

    /**
     * 删除菜品分类
     * DELETE /api/merchant/categories/{categoryId}
     */
    @DeleteMapping("/categories/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Integer categoryId) {
        productService.deleteCategory(getCurrentUserId(), categoryId);
        return ResponseEntity.ok(ApiResponse.success("分类删除成功", null));
    }

    // ==================== 商品管理 ====================

    /**
     * 查看自己店铺的商品列表（支持按分类筛选）
     * GET /api/merchant/products?categoryId=1
     */
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getMyProducts(
            @RequestParam(required = false) Integer categoryId) {
        List<ProductResponse> products = productService.getMyProducts(getCurrentUserId(), categoryId);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    /**
     * 添加商品
     * POST /api/merchant/products
     */
    @PostMapping("/products")
    public ResponseEntity<ApiResponse<ProductResponse>> addProduct(
            @Valid @RequestBody ProductRequest request) {
        ProductResponse product = productService.addProduct(getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("商品添加成功", product));
    }

    /**
     * 编辑商品
     * PUT /api/merchant/products/{productId}
     */
    @PutMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Integer productId,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse product = productService.updateProduct(getCurrentUserId(), productId, request);
        return ResponseEntity.ok(ApiResponse.success("商品更新成功", product));
    }

    /**
     * 删除商品
     * DELETE /api/merchant/products/{productId}
     */
    @DeleteMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Integer productId) {
        productService.deleteProduct(getCurrentUserId(), productId);
        return ResponseEntity.ok(ApiResponse.success("商品删除成功", null));
    }

    // ==================== 图片上传 ====================

    /**
     * 上传菜品图片
     * POST /api/merchant/upload/image
     *
     * @param file 图片文件（表单字段名: image）
     * @return 上传结果，包含可访问的图片 URL
     */
    @PostMapping("/upload/image")
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadImage(
            @RequestParam("image") MultipartFile file) {
        ImageUploadResponse result = fileUploadService.uploadImage(file);
        return ResponseEntity.ok(ApiResponse.success("图片上传成功", result));
    }
}
