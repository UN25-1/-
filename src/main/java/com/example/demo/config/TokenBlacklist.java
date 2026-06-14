package com.example.demo.config;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token 黑名单 —— 实现登出时Token失效
 * 使用内存存储，定期清理过期条目
 */
@Component
public class TokenBlacklist {

    /** token -> 过期时间戳(毫秒) */
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    /** 默认Token存活时间（毫秒），与JwtUtils保持一致 */
    private static final long DEFAULT_TTL_MS = 24 * 60 * 60 * 1000;

    /**
     * 将Token加入黑名单
     */
    public void add(String token, long expireAtMillis) {
        blacklist.put(token, expireAtMillis);
    }

    /**
     * 将Token加入黑名单（使用默认过期时间）
     */
    public void add(String token) {
        blacklist.put(token, System.currentTimeMillis() + DEFAULT_TTL_MS);
    }

    /**
     * 检查Token是否在黑名单中（已失效）
     */
    public boolean isBlacklisted(String token) {
        Long expireAt = blacklist.get(token);
        if (expireAt == null) {
            return false;
        }
        // 已过期则自动移除
        if (System.currentTimeMillis() > expireAt) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }

    /**
     * 定时清理过期Token（每小时执行一次）
     */
    @Scheduled(fixedRate = 3600000)
    public void cleanExpired() {
        long now = System.currentTimeMillis();
        blacklist.entrySet().removeIf(entry -> now > entry.getValue());
    }
}
