package com.enterprise.agent.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 风控控制器
 */
@Component
public class RiskController {

    private static final Logger log = LoggerFactory.getLogger(RiskController.class);

    private final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 60;

    /**
     * 风控检查
     */
    public boolean checkRisk(String userId, String message) {
        // 1. 限流检查
        if (!checkRateLimit(userId)) {
            log.warn("用户请求频率超限: {}", userId);
            return false;
        }

        // 2. 消息长度检查
        if (message != null && message.length() > 2000) {
            log.warn("消息过长: userId={}, length={}", userId, message.length());
            return false;
        }

        // 3. 注入检测
        if (containsInjection(message)) {
            log.warn("检测到注入攻击: userId={}", userId);
            return false;
        }

        return true;
    }

    private boolean checkRateLimit(String userId) {
        AtomicInteger count = requestCounts.computeIfAbsent(userId, k -> new AtomicInteger(0));
        return count.incrementAndGet() <= MAX_REQUESTS_PER_MINUTE;
    }

    private boolean containsInjection(String message) {
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();

        // SQL 注入检测
        if (lower.contains("select ") && lower.contains(" from ")) {
            return true;
        }
        if (lower.contains("drop table") || lower.contains("truncate")) {
            return true;
        }

        // XSS 检测
        if (lower.contains("<script") || lower.contains("javascript:")) {
            return true;
        }

        return false;
    }

    /**
     * 重置限流计数
     */
    public void resetRateLimits() {
        requestCounts.clear();
    }
}
