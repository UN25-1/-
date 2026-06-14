package com.example.demo.service;

import com.example.demo.dto.CartItemRequest;
import com.example.demo.dto.CartItemResponse;
import com.example.demo.dto.CartMerchantGroup;
import com.example.demo.entity.CartItem;
import com.example.demo.entity.MerchantDetail;
import com.example.demo.entity.Product;
import com.example.demo.exception.BusinessException;
import com.example.demo.repository.CartItemRepository;
import com.example.demo.repository.MerchantDetailRepository;
import com.example.demo.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 购物车服务 —— 添加/修改/删除/查询/跨商家校验
 * 所有数据库操作均通过 JPA 参数化查询执行，防止SQL注入
 */
@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final MerchantDetailRepository merchantDetailRepository;

    public CartService(CartItemRepository cartItemRepository,
                       ProductRepository productRepository,
                       MerchantDetailRepository merchantDetailRepository) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.merchantDetailRepository = merchantDetailRepository;
    }

    // ==================== 购物车CRUD ====================

    /**
     * 添加商品到购物车（通过 DTO）
     * 同一用户+同一商品则累加数量（由数据库 UNIQUE 约束兜底）
     */
    @Transactional
    public CartItemResponse addToCart(Integer userId, CartItemRequest request) {
        return addToCart(userId, request.getProductId(), request.getQuantity());
    }

    /**
     * 添加商品到购物车（直接传 productId + quantity）
     * 重复添加时自动累加数量，不会报错
     *
     * @param userId    用户ID
     * @param productId 商品ID
     * @param quantity  添加数量
     * @return 购物车项响应（isNewItem=true 表示新增，false 表示累加）
     */
    @Transactional
    public CartItemResponse addToCart(Integer userId, Integer productId, int quantity) {
        if (productId == null) {
            throw new BusinessException(400, "商品ID不能为空");
        }
        if (quantity < 1) {
            throw new BusinessException(400, "数量至少为1");
        }

        // 1. 校验商品存在且未下架
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(404, "商品不存在"));
        if (!Boolean.TRUE.equals(product.getIsAvailable())) {
            throw new BusinessException(400, "该商品已下架");
        }

        // 2. 校验库存上限（防止添加超出库存的商品到购物车）
        Optional<CartItem> existing = cartItemRepository.findByUserIdAndProductId(userId, productId);
        int currentCartQty = existing.map(CartItem::getQuantity).orElse(0);
        int newTotalQty = currentCartQty + quantity;

        if (newTotalQty > product.getStock()) {
            throw new BusinessException(400, "商品[" + product.getName()
                    + "]库存不足，当前库存" + product.getStock()
                    + "件，购物车已有" + currentCartQty + "件，最多还能添加"
                    + Math.max(0, product.getStock() - currentCartQty) + "件");
        }

        // 3. 累加数量或新增
        CartItem cartItem;
        boolean isNewItem;
        if (existing.isPresent()) {
            cartItem = existing.get();
            cartItem.setQuantity(newTotalQty);
            isNewItem = false;
            log.info("购物车累加数量：userId={}, productId={}, 原数量={}, 新增{}, 新数量={}",
                    userId, productId, currentCartQty, quantity, newTotalQty);
        } else {
            cartItem = new CartItem(userId, productId, quantity);
            isNewItem = true;
            log.info("购物车新增商品：userId={}, productId={}, quantity={}", userId, productId, quantity);
        }
        cartItem = cartItemRepository.save(cartItem);

        CartItemResponse response = buildCartItemResponse(cartItem, product);
        response.setIsNewItem(isNewItem);
        return response;
    }

    /**
     * 修改购物车商品数量
     */
    @Transactional
    public CartItemResponse updateQuantity(Integer userId, Integer productId, int quantity) {
        CartItem cartItem = cartItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new BusinessException(404, "购物车中不存在该商品"));

        cartItem.setQuantity(quantity);
        cartItem = cartItemRepository.save(cartItem);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(404, "商品不存在"));

        log.info("购物车更新数量：userId={}, productId={}, quantity={}", userId, productId, quantity);
        return buildCartItemResponse(cartItem, product);
    }

    /**
     * 删除购物车中的指定商品
     */
    @Transactional
    public void removeFromCart(Integer userId, Integer productId) {
        if (!cartItemRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new BusinessException(404, "购物车中不存在该商品");
        }
        cartItemRepository.deleteByUserIdAndProductId(userId, productId);
        log.info("购物车删除商品：userId={}, productId={}", userId, productId);
    }

    /**
     * 查询我的购物车（按商家分组展示）
     */
    public List<CartMerchantGroup> getMyCart(Integer userId) {
        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (cartItems.isEmpty()) {
            return Collections.emptyList();
        }

        // 收集所有 productId，批量查询商品和商家信息
        List<Integer> productIds = cartItems.stream()
                .map(CartItem::getProductId)
                .collect(Collectors.toList());

        List<Product> products = productRepository.findAllById(productIds);
        Map<Integer, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        Set<Integer> merchantIds = products.stream()
                .map(Product::getMerchantId)
                .collect(Collectors.toSet());
        Map<Integer, MerchantDetail> merchantMap = merchantDetailRepository.findAllById(merchantIds)
                .stream()
                .collect(Collectors.toMap(MerchantDetail::getId, m -> m));

        // 构建 CartItemResponse 列表
        List<CartItemResponse> itemResponses = cartItems.stream()
                .map(item -> {
                    Product product = productMap.get(item.getProductId());
                    if (product == null) return null;
                    MerchantDetail merchant = merchantMap.get(product.getMerchantId());
                    return CartItemResponse.of(
                            item.getId(),
                            product.getId(),
                            product.getName(),
                            product.getImageUrl(),
                            product.getPrice(),
                            item.getQuantity(),
                            product.getMerchantId(),
                            merchant != null ? merchant.getShopName() : "未知商家"
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 按商家分组
        return groupByMerchant(itemResponses, merchantMap);
    }

    // ==================== 跨商家校验 ====================

    /**
     * 跨商家校验：检测购物车中是否包含多个商家的商品
     * @return 按商家分组的 Map，若 >1 个商家则提示分别下单
     */
    public Map<String, Object> checkCrossMerchant(Integer userId) {
        List<CartMerchantGroup> merchantGroups = getMyCart(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("totalMerchants", merchantGroups.size());
        result.put("merchantGroups", merchantGroups);

        if (merchantGroups.size() > 1) {
            result.put("hasCrossMerchant", true);
            result.put("warning", "购物车中包含" + merchantGroups.size()
                    + "个商家的商品，请分别下单。系统将自动按商家拆分订单。");
        } else {
            result.put("hasCrossMerchant", false);
            result.put("warning", null);
        }

        return result;
    }

    /**
     * 按商家拆分购物车商品（供订单模块调用）
     * @return Map<merchantId, List<CartItemResponse>>
     */
    public Map<Integer, List<CartItemResponse>> splitByMerchant(Integer userId) {
        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId);

        List<Integer> productIds = cartItems.stream()
                .map(CartItem::getProductId)
                .collect(Collectors.toList());
        List<Product> products = productRepository.findAllById(productIds);
        Map<Integer, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        return cartItems.stream()
                .map(item -> {
                    Product product = productMap.get(item.getProductId());
                    if (product == null) return null;
                    return CartItemResponse.of(
                            item.getId(),
                            product.getId(),
                            product.getName(),
                            product.getImageUrl(),
                            product.getPrice(),
                            item.getQuantity(),
                            product.getMerchantId(),
                            null
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(CartItemResponse::getMerchantId));
    }

    // ==================== 结算后清空 ====================

    /**
     * 下单成功后，清空对应购物车项（供订单模块调用）
     * @param userId 用户ID
     * @param productIds 需要清空的商品ID列表
     */
    @Transactional
    public void clearCartAfterOrder(Integer userId, List<Integer> productIds) {
        cartItemRepository.deleteByUserIdAndProductIdIn(userId, productIds);
        log.info("下单后清空购物车：userId={}, productIds={}", userId, productIds);
    }

    /**
     * 清空用户所有购物车商品
     */
    @Transactional
    public void clearAllCart(Integer userId) {
        cartItemRepository.deleteAllByUserId(userId);
        log.info("清空购物车：userId={}", userId);
    }

    // ==================== 内部工具方法 ====================

    /**
     * 构建单个购物车项的响应DTO
     */
    private CartItemResponse buildCartItemResponse(CartItem cartItem, Product product) {
        MerchantDetail merchant = merchantDetailRepository.findById(product.getMerchantId())
                .orElse(null);

        return CartItemResponse.of(
                cartItem.getId(),
                product.getId(),
                product.getName(),
                product.getImageUrl(),
                product.getPrice(),
                cartItem.getQuantity(),
                product.getMerchantId(),
                merchant != null ? merchant.getShopName() : "未知商家"
        );
    }

    /**
     * 将购物车项按商家分组聚合
     */
    private List<CartMerchantGroup> groupByMerchant(List<CartItemResponse> items,
                                                     Map<Integer, MerchantDetail> merchantMap) {
        // 按 merchantId 分组
        Map<Integer, List<CartItemResponse>> grouped = items.stream()
                .collect(Collectors.groupingBy(
                        CartItemResponse::getMerchantId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<CartMerchantGroup> groups = new ArrayList<>();
        for (Map.Entry<Integer, List<CartItemResponse>> entry : grouped.entrySet()) {
            Integer merchantId = entry.getKey();
            List<CartItemResponse> merchantItems = entry.getValue();

            CartMerchantGroup group = new CartMerchantGroup();
            group.setMerchantId(merchantId);
            group.setItems(merchantItems);

            // 计算该商家商品小计
            BigDecimal subtotal = merchantItems.stream()
                    .map(CartItemResponse::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            group.setSubtotal(subtotal);

            // 填充商家信息与配送规则（确保不为null，防止前端JSON NaN）
            MerchantDetail merchant = merchantMap.get(merchantId);
            if (merchant != null) {
                group.setMerchantName(merchant.getShopName());
                group.setDeliveryFee(merchant.getDeliveryFee());
                group.setMinOrderAmount(merchant.getMinOrderAmount());
            } else {
                group.setMerchantName("未知商家");
                // deliveryFee/minOrderAmount 已在 CartMerchantGroup 中默认为 BigDecimal.ZERO，此处安全
            }

            groups.add(group);
        }

        return groups;
    }
}
