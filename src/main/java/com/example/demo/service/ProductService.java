package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.entity.MerchantDetail;
import com.example.demo.entity.Product;
import com.example.demo.entity.ProductCategory;
import com.example.demo.exception.BusinessException;
import com.example.demo.repository.CartItemRepository;
import com.example.demo.repository.MerchantDetailRepository;
import com.example.demo.repository.ProductCategoryRepository;
import com.example.demo.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品服务 —— 分类管理 + 商品管理 + 用户端浏览
 * 所有数据库操作均通过 JPA 参数化查询执行，防止SQL注入
 */
@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final MerchantDetailRepository merchantDetailRepository;
    private final CartItemRepository cartItemRepository;

    public ProductService(ProductRepository productRepository,
                          ProductCategoryRepository categoryRepository,
                          MerchantDetailRepository merchantDetailRepository,
                          CartItemRepository cartItemRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.merchantDetailRepository = merchantDetailRepository;
        this.cartItemRepository = cartItemRepository;
    }

    // ==================== 分类管理（商家端） ====================

    /**
     * 添加菜品分类
     */
    @Transactional
    public ProductCategoryResponse addCategory(Integer merchantUserId, ProductCategoryRequest request) {
        Integer merchantId = getMerchantIdByUserId(merchantUserId);

        if (categoryRepository.existsByMerchantIdAndName(merchantId, request.getName())) {
            throw new BusinessException(409, "分类名称已存在，请更换");
        }

        ProductCategory category = new ProductCategory();
        category.setMerchantId(merchantId);
        category.setName(request.getName());
        category.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        category.setIsAvailable(request.getIsAvailable() != null ? request.getIsAvailable() : true);

        category = categoryRepository.save(category);
        log.info("分类添加成功：id={}, merchantId={}, name={}", category.getId(), merchantId, category.getName());
        return ProductCategoryResponse.from(category);
    }

    /**
     * 编辑菜品分类
     */
    @Transactional
    public ProductCategoryResponse updateCategory(Integer merchantUserId, Integer categoryId,
                                                   ProductCategoryRequest request) {
        Integer merchantId = getMerchantIdByUserId(merchantUserId);
        ProductCategory category = validateCategoryOwnership(merchantId, categoryId);

        // 如果改名，检查新名字是否与同商家其他分类冲突
        if (!category.getName().equals(request.getName())
                && categoryRepository.existsByMerchantIdAndName(merchantId, request.getName())) {
            throw new BusinessException(409, "分类名称已存在，请更换");
        }

        category.setName(request.getName());
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }
        if (request.getIsAvailable() != null) {
            category.setIsAvailable(request.getIsAvailable());
        }

        category = categoryRepository.save(category);
        log.info("分类更新成功：id={}, merchantId={}", categoryId, merchantId);
        return ProductCategoryResponse.from(category);
    }

    /**
     * 删除菜品分类（关联的商品 category_id 会被设为 NULL）
     */
    @Transactional
    public void deleteCategory(Integer merchantUserId, Integer categoryId) {
        Integer merchantId = getMerchantIdByUserId(merchantUserId);
        validateCategoryOwnership(merchantId, categoryId);
        categoryRepository.deleteById(categoryId);
        log.info("分类删除成功：id={}, merchantId={}", categoryId, merchantId);
    }

    /**
     * 查询自己店铺的所有分类
     */
    public List<ProductCategoryResponse> getMyCategories(Integer merchantUserId) {
        Integer merchantId = getMerchantIdByUserId(merchantUserId);
        return categoryRepository.findByMerchantIdOrderBySortOrderAsc(merchantId)
                .stream()
                .map(ProductCategoryResponse::from)
                .collect(Collectors.toList());
    }

    // ==================== 商品管理（商家端） ====================

    /**
     * 添加商品
     */
    @Transactional
    public ProductResponse addProduct(Integer merchantUserId, ProductRequest request) {
        Integer merchantId = getMerchantIdByUserId(merchantUserId);

        // 如果指定了分类，校验分类归属
        if (request.getCategoryId() != null) {
            if (!categoryRepository.existsByIdAndMerchantId(request.getCategoryId(), merchantId)) {
                throw new BusinessException(400, "所选分类不属于您的店铺");
            }
        }

        Product product = new Product();
        product.setMerchantId(merchantId);
        product.setCategoryId(request.getCategoryId());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setIsAvailable(request.getIsAvailable() != null ? request.getIsAvailable() : true);
        if (request.getStock() != null) {
            product.setStock(request.getStock());
        }

        product = productRepository.save(product);
        log.info("商品添加成功：id={}, merchantId={}, name={}", product.getId(), merchantId, product.getName());
        return buildProductResponse(product);
    }

    /**
     * 编辑商品
     */
    @Transactional
    public ProductResponse updateProduct(Integer merchantUserId, Integer productId, ProductRequest request) {
        Integer merchantId = getMerchantIdByUserId(merchantUserId);
        Product product = validateProductOwnership(merchantId, productId);

        // 如果更改分类，校验分类归属
        if (request.getCategoryId() != null) {
            if (!categoryRepository.existsByIdAndMerchantId(request.getCategoryId(), merchantId)) {
                throw new BusinessException(400, "所选分类不属于您的店铺");
            }
            product.setCategoryId(request.getCategoryId());
        }

        product.setName(request.getName());
        if (request.getDescription() != null)
            product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        if (request.getImageUrl() != null)
            product.setImageUrl(request.getImageUrl());
        if (request.getIsAvailable() != null)
            product.setIsAvailable(request.getIsAvailable());
        if (request.getStock() != null)
            product.setStock(request.getStock());

        product = productRepository.save(product);
        log.info("商品更新成功：id={}, merchantId={}", productId, merchantId);
        return buildProductResponse(product);
    }

    /**
     * 删除商品
     */
    @Transactional
    public void deleteProduct(Integer merchantUserId, Integer productId) {
        Integer merchantId = getMerchantIdByUserId(merchantUserId);
        validateProductOwnership(merchantId, productId);

        // 删除商品
        productRepository.deleteById(productId);

        // 联动清理所有用户购物车中该商品的记录
        cartItemRepository.deleteByProductId(productId);

        log.info("商品删除成功（含购物车联动清理）：id={}, merchantId={}", productId, merchantId);
    }

    /**
     * 查询自己店铺的所有商品
     */
    public List<ProductResponse> getMyProducts(Integer merchantUserId, Integer categoryId) {
        Integer merchantId = getMerchantIdByUserId(merchantUserId);
        List<Product> products;
        if (categoryId != null) {
            products = productRepository.findByMerchantIdAndCategoryId(merchantId, categoryId);
        } else {
            products = productRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
        }
        return products.stream().map(this::buildProductResponse).collect(Collectors.toList());
    }

    // ==================== 用户端浏览（公开） ====================

    /**
     * 商家列表（支持搜索与按评分排序）
     */
    public List<MerchantDetailResponse> getMerchantList(String keyword) {
        List<MerchantDetail> merchants;
        if (keyword != null && !keyword.trim().isEmpty()) {
            merchants = merchantDetailRepository.findByShopNameContaining(keyword.trim());
        } else {
            merchants = merchantDetailRepository.findAllByOrderByRatingDesc();
        }
        return merchants.stream().map(MerchantDetailResponse::from).collect(Collectors.toList());
    }

    /**
     * 商家详情
     */
    public MerchantDetailResponse getMerchantDetail(Integer merchantId) {
        MerchantDetail detail = merchantDetailRepository.findById(merchantId)
                .orElseThrow(() -> new BusinessException(404, "商家不存在"));
        return MerchantDetailResponse.from(detail);
    }

    /**
     * 某商家的分类列表（用户端公开：仅返回上架分类）
     */
    public List<ProductCategoryResponse> getCategoriesByMerchant(Integer merchantId) {
        if (!merchantDetailRepository.existsById(merchantId)) {
            throw new BusinessException(404, "商家不存在");
        }
        // 用户端仅展示上架的分类
        return categoryRepository.findByMerchantIdAndIsAvailableTrueOrderBySortOrderAsc(merchantId)
                .stream()
                .map(ProductCategoryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 某商家的商品列表（默认只看上架商品，支持按分类筛选）
     */
    public List<ProductResponse> getProductsByMerchant(Integer merchantId, Integer categoryId, Boolean showAll) {
        if (!merchantDetailRepository.existsById(merchantId)) {
            throw new BusinessException(404, "商家不存在");
        }

        List<Product> products;
        boolean onlyAvailable = showAll == null || !showAll;

        if (categoryId != null) {
            products = onlyAvailable
                    ? productRepository.findByMerchantIdAndCategoryIdAndIsAvailableTrue(merchantId, categoryId)
                    : productRepository.findByMerchantIdAndCategoryId(merchantId, categoryId);
        } else {
            products = onlyAvailable
                    ? productRepository.findByMerchantIdAndIsAvailableTrue(merchantId)
                    : productRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
        }

        return products.stream().map(this::buildProductResponse).collect(Collectors.toList());
    }

    /**
     * 商品详情（公开接口）
     * 已下架商品仅商家本人或管理员可查看
     */
    public ProductResponse getProductDetail(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(404, "商品不存在"));

        // 已下架商品：仅商家本人或管理员可查看
        if (Boolean.FALSE.equals(product.getIsAvailable())) {
            var auth = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()
                    || "anonymousUser".equals(auth.getPrincipal())) {
                throw new BusinessException(404, "商品不存在或已下架");
            }
            String currentRole = auth.getAuthorities().stream()
                    .findFirst().map(Object::toString).orElse("");
            if (!currentRole.contains("ROLE_MERCHANT") && !currentRole.contains("ROLE_ADMIN")) {
                throw new BusinessException(404, "商品不存在或已下架");
            }
            // 如果是商家，校验是否是该商家的商品
            if (currentRole.contains("ROLE_MERCHANT")) {
                Integer currentUserId = (Integer) auth.getPrincipal();
                merchantDetailRepository.findByUserId(currentUserId).ifPresentOrElse(
                        m -> {
                            if (!m.getId().equals(product.getMerchantId())) {
                                throw new BusinessException(404, "商品不存在或已下架");
                            }
                        },
                        () -> { throw new BusinessException(404, "商品不存在或已下架"); }
                );
            }
        }

        return buildProductResponse(product);
    }

    /**
     * 搜索商品（跨商家）
     */
    public List<ProductResponse> searchProducts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new BusinessException(400, "搜索关键词不能为空");
        }
        return productRepository.findByNameContainingAndIsAvailableTrue(keyword.trim())
                .stream()
                .map(this::buildProductResponse)
                .collect(Collectors.toList());
    }

    // ==================== 内部工具方法 ====================

    /**
     * 根据用户ID获取商家详情ID（同时校验用户是否已创建店铺）
     */
    private Integer getMerchantIdByUserId(Integer userId) {
        MerchantDetail detail = merchantDetailRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "您还未创建店铺，请先入驻"));
        return detail.getId();
    }

    /**
     * 校验分类归属权
     */
    private ProductCategory validateCategoryOwnership(Integer merchantId, Integer categoryId) {
        ProductCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(404, "分类不存在"));
        if (!category.getMerchantId().equals(merchantId)) {
            throw new BusinessException(403, "无权操作该分类");
        }
        return category;
    }

    /**
     * 校验商品归属权
     */
    private Product validateProductOwnership(Integer merchantId, Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(404, "商品不存在"));
        if (!product.getMerchantId().equals(merchantId)) {
            throw new BusinessException(403, "无权操作该商品");
        }
        return product;
    }

    /**
     * 构建商品响应（附带商家名称和分类名称）
     */
    private ProductResponse buildProductResponse(Product product) {
        ProductResponse resp = ProductResponse.from(product);

        // 附带商家名称
        merchantDetailRepository.findById(product.getMerchantId())
                .ifPresent(m -> resp.setMerchantName(m.getShopName()));

        // 附带分类名称
        if (product.getCategoryId() != null) {
            categoryRepository.findById(product.getCategoryId())
                    .ifPresent(c -> resp.setCategoryName(c.getName()));
        }

        return resp;
    }
}
