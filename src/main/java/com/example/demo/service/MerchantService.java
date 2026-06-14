package com.example.demo.service;

import com.example.demo.dto.MerchantDetailRequest;
import com.example.demo.dto.MerchantDetailResponse;
import com.example.demo.entity.MerchantDetail;
import com.example.demo.exception.BusinessException;
import com.example.demo.repository.MerchantDetailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 商家服务 —— 商家入驻 + 店铺信息管理
 * 所有数据库操作均通过 JPA 参数化查询执行，防止SQL注入
 */
@Service
public class MerchantService {

    private static final Logger log = LoggerFactory.getLogger(MerchantService.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm[:ss]");

    private final MerchantDetailRepository merchantDetailRepository;

    public MerchantService(MerchantDetailRepository merchantDetailRepository) {
        this.merchantDetailRepository = merchantDetailRepository;
    }

    /**
     * 商家入驻/完善店铺信息（一个用户只能有一个店铺）
     * 如果已存在则更新，不存在则创建
     */
    @Transactional
    public MerchantDetailResponse createOrUpdateDetail(Integer userId, MerchantDetailRequest request) {
        MerchantDetail detail = merchantDetailRepository.findByUserId(userId)
                .orElseGet(() -> {
                    MerchantDetail newDetail = new MerchantDetail();
                    newDetail.setUserId(userId);
                    newDetail.setRating(new BigDecimal("5.00"));
                    return newDetail;
                });

        applyRequestToDetail(detail, request);

        boolean isNew = detail.getId() == null;
        detail = merchantDetailRepository.save(detail);

        log.info("商家{}成功：id={}, userId={}", isNew ? "入驻" : "信息更新", detail.getId(), userId);
        return MerchantDetailResponse.from(detail);
    }

    /**
     * 查看自己的店铺信息
     */
    public MerchantDetailResponse getMyDetail(Integer userId) {
        MerchantDetail detail = merchantDetailRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "您还未创建店铺，请先入驻"));
        return MerchantDetailResponse.from(detail);
    }

    /**
     * 编辑店铺信息
     */
    @Transactional
    public MerchantDetailResponse updateDetail(Integer userId, MerchantDetailRequest request) {
        MerchantDetail detail = merchantDetailRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "您还未创建店铺，请先入驻"));

        applyRequestToDetail(detail, request);
        detail = merchantDetailRepository.save(detail);

        log.info("店铺信息更新成功：id={}, userId={}", detail.getId(), userId);
        return MerchantDetailResponse.from(detail);
    }

    // ==================== 内部工具方法 ====================

    private void applyRequestToDetail(MerchantDetail detail, MerchantDetailRequest request) {
        detail.setShopName(request.getShopName());

        if (request.getShopAddress() != null)
            detail.setShopAddress(request.getShopAddress());

        if (request.getShopPhone() != null)
            detail.setShopPhone(request.getShopPhone());

        if (request.getDescription() != null)
            detail.setDescription(request.getDescription());

        if (request.getLogoUrl() != null)
            detail.setLogoUrl(request.getLogoUrl());

        if (request.getOpeningTime() != null)
            detail.setOpeningTime(parseTimeSafely(request.getOpeningTime(), "营业开始时间"));

        if (request.getClosingTime() != null)
            detail.setClosingTime(parseTimeSafely(request.getClosingTime(), "营业结束时间"));

        if (request.getDeliveryFee() != null)
            detail.setDeliveryFee(request.getDeliveryFee());

        if (request.getMinOrderAmount() != null)
            detail.setMinOrderAmount(request.getMinOrderAmount());
    }

    private LocalTime parseTimeSafely(String timeStr, String fieldName) {
        try {
            return LocalTime.parse(timeStr, TIME_FMT);
        } catch (DateTimeParseException e) {
            throw new BusinessException(400, fieldName + "格式错误，请使用 HH:mm 格式（如 09:00）");
        }
    }
}
