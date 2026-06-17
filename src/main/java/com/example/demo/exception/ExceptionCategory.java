package com.example.demo.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 异常类型分类工具 —— 根据 OrderStatusLog.remark 自动识别异常分类
 * <p>
 * 注意：由于数据库 order_status = 'exception' 没有细分类型字段，
 * 本工具通过分析 remark 文本中的关键词进行分类映射。
 */
public final class ExceptionCategory {

    // ==================== 异常类型常量 ====================

    /** 配送超时 — 系统自动检测配送中超过60分钟 */
    public static final String DELIVERY_TIMEOUT = "delivery_timeout";
    /** 骑手上报 — 骑手主动上报的配送异常（用户失联、地址错误等） */
    public static final String RIDER_REPORTED = "rider_reported";
    /** 支付失败 — 支付环节出现异常 */
    public static final String PAYMENT_FAILED = "payment_failed";
    /** 库存不足 — 商品库存问题 */
    public static final String STOCK_SHORTAGE = "stock_shortage";
    /** 地址错误 — 配送地址存在问题 */
    public static final String ADDRESS_ERROR = "address_error";
    /** 退款异常 — 退款流程出现异常 */
    public static final String REFUND_ERROR = "refund_error";
    /** 其他/未知异常 */
    public static final String OTHER = "other";

    // ==================== 中文标签映射 ====================

    private static final Map<String, String> TYPE_LABELS = new LinkedHashMap<>();

    static {
        TYPE_LABELS.put(DELIVERY_TIMEOUT, "配送超时");
        TYPE_LABELS.put(RIDER_REPORTED,  "骑手上报");
        TYPE_LABELS.put(PAYMENT_FAILED,  "支付异常");
        TYPE_LABELS.put(STOCK_SHORTAGE,  "库存异常");
        TYPE_LABELS.put(ADDRESS_ERROR,   "地址异常");
        TYPE_LABELS.put(REFUND_ERROR,    "退款异常");
        TYPE_LABELS.put(OTHER,           "其他异常");
    }

    // ==================== 处理建议映射 ====================

    private static final Map<String, String> SUGGESTIONS = new LinkedHashMap<>();

    static {
        SUGGESTIONS.put(DELIVERY_TIMEOUT, "请联系商家或骑手了解配送情况，或重新下单");
        SUGGESTIONS.put(RIDER_REPORTED,  "骑手已上报异常，请耐心等待平台处理或联系客服");
        SUGGESTIONS.put(PAYMENT_FAILED,  "请核对支付信息后重试，如已扣款请联系客服退款");
        SUGGESTIONS.put(STOCK_SHORTAGE,  "商品暂时缺货，建议联系商家确认补货时间或选择其他商品");
        SUGGESTIONS.put(ADDRESS_ERROR,   "请核对并修改收货地址后重新下单");
        SUGGESTIONS.put(REFUND_ERROR,    "退款流程异常，请联系客服处理退款事宜");
        SUGGESTIONS.put(OTHER,           "订单出现异常，请联系客服获取帮助");
    }

    // ==================== 关键词分类规则 ====================

    private static final LinkedHashMap<String, Pattern> CLASSIFICATION_RULES = new LinkedHashMap<>();

    static {
        CLASSIFICATION_RULES.put(DELIVERY_TIMEOUT, Pattern.compile("超时|timeout|超时.*标记|配送超时|超时.*取消"));
        CLASSIFICATION_RULES.put(PAYMENT_FAILED,   Pattern.compile("支付.*失败|支付.*异常|付款.*失败|payment.*fail|扣款.*异常"));
        CLASSIFICATION_RULES.put(STOCK_SHORTAGE,   Pattern.compile("库存.*不足|缺货|售罄|stock|库存.*异常|下架"));
        CLASSIFICATION_RULES.put(ADDRESS_ERROR,    Pattern.compile("地址.*错误|地址.*无效|地址.*异常|超出.*范围|不在.*配送|定位.*失败"));
        CLASSIFICATION_RULES.put(REFUND_ERROR,     Pattern.compile("退款.*失败|退款.*异常|退款.*超时|refund.*fail|退款.*问题"));
        CLASSIFICATION_RULES.put(RIDER_REPORTED,   Pattern.compile("骑手.*上报|骑手.*报告|用户.*失联|联系.*不上|无法.*送达|拒收"));
    }

    private ExceptionCategory() {}

    // ==================== 公共方法 ====================

    /**
     * 根据 remark 文本分类异常类型
     *
     * @param remark 状态日志的备注文本（可为null）
     * @return 异常类型常量
     */
    public static String classify(String remark) {
        if (remark == null || remark.isBlank()) {
            return OTHER;
        }
        for (Map.Entry<String, Pattern> entry : CLASSIFICATION_RULES.entrySet()) {
            if (entry.getValue().matcher(remark).find()) {
                return entry.getKey();
            }
        }
        return OTHER;
    }

    /**
     * 获取异常类型的中文标签
     */
    public static String getLabel(String exceptionType) {
        return TYPE_LABELS.getOrDefault(exceptionType, "未知异常");
    }

    /**
     * 获取异常处理建议
     */
    public static String getSuggestion(String exceptionType) {
        return SUGGESTIONS.getOrDefault(exceptionType, "订单出现异常，请联系客服获取帮助");
    }

    /**
     * 获取所有异常类型的中文标签映射（供前端使用）
     */
    public static Map<String, String> getAllLabels() {
        return new LinkedHashMap<>(TYPE_LABELS);
    }
}
