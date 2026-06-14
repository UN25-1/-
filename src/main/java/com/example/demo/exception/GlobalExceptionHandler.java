package com.example.demo.exception;

import com.example.demo.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 统一处理各类异常，返回一致的 ApiResponse 格式
 * 接口调用失败时输出详细错误日志到终端，便于排查问题
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String SEPARATOR = "============================================================";
    private static final String SUB_SEPARATOR = "------------------------------------------------------------";

    /**
     * 输出详细的接口调用失败信息到终端
     */
    private void logErrorDetail(HttpServletRequest request, int httpStatus, String reason, Exception ex) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String method = request.getMethod();
        String url = request.getRequestURI();
        String queryString = request.getQueryString();
        String fullUrl = queryString != null ? url + "?" + queryString : url;
        String clientIp = getClientIp(request);
        String exceptionType = ex != null ? ex.getClass().getName() : "N/A";
        String rootCause = ex != null ? extractRootCause(ex) : "N/A";

        System.err.println();
        System.err.println(SEPARATOR);
        System.err.println("  [!] 接口调用失败");
        System.err.println(SEPARATOR);
        System.err.println("  发生时间 : " + timestamp);
        System.err.println("  请求地址 : " + method + " " + fullUrl);
        System.err.println("  客户端IP : " + clientIp);
        System.err.println("  HTTP状态码 : " + httpStatus + " (" + getHttpStatusText(httpStatus) + ")");
        System.err.println(SUB_SEPARATOR);
        System.err.println("  失败原因 : " + reason);
        System.err.println("  异常类型 : " + exceptionType);
        System.err.println("  根因描述 : " + rootCause);
        System.err.println(SEPARATOR);
        System.err.println();

        // 同时输出到 SLF4J 日志
        log.error("[接口失败] 时间={} | 地址={} {} | 客户端IP={} | HTTP状态码={} | 原因={} | 异常类型={} | 根因={}",
                timestamp, method, fullUrl, clientIp, httpStatus, reason, exceptionType, rootCause, ex);
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 提取异常根因信息
     */
    private String extractRootCause(Exception ex) {
        if (ex == null) return "N/A";
        Throwable cause = ex;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String msg = cause.getMessage();
        return msg != null ? msg : "(无详细信息)";
    }

    /**
     * 获取HTTP状态码对应的文本描述
     */
    private String getHttpStatusText(int code) {
        try {
            return HttpStatus.valueOf(code).getReasonPhrase();
        } catch (IllegalArgumentException e) {
            return "Unknown";
        }
    }

    /**
     * 处理参数校验异常（@Valid 校验失败）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        // 打印原始请求体对象，便于排查 Android 实际发送的字段名和值
        Object target = ex.getBindingResult().getTarget();
        System.err.println(">>> [调试] 请求体解析结果: " + target);

        String reason = "参数校验失败: " + errors;
        logErrorDetail(request, HttpStatus.BAD_REQUEST.value(), reason, ex);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, reason));
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex,
                                                                      HttpServletRequest request) {
        logErrorDetail(request, ex.getCode(), ex.getMessage(), ex);
        return ResponseEntity.status(ex.getCode())
                .body(ApiResponse.error(ex.getCode(), ex.getMessage()));
    }

    /**
     * 处理其他未预期异常，防止敏感信息泄露
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex,
                                                                     HttpServletRequest request) {
        logErrorDetail(request, HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务器内部错误", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "服务器内部错误，请稍后重试"));
    }
}
