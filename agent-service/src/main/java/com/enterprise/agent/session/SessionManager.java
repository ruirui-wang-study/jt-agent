package com.enterprise.agent.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话管理器
 */
@Component
public class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);

    private final Map<String, ConversationContext> localCache = new ConcurrentHashMap<>();
    private static final int SESSION_TIMEOUT_MINUTES = 30;

    /**
     * 获取或创建会话
     */
    public ConversationContext getOrCreate(String sessionId, String userId) {
        ConversationContext context = get(sessionId);

        if (context == null) {
            context = create(sessionId, userId);
        } else {
            context.setLastActiveTime(LocalDateTime.now());
            context.setTraceId(generateTraceId());
        }

        return context;
    }

    /**
     * 获取会话
     */
    public ConversationContext get(String sessionId) {
        ConversationContext context = localCache.get(sessionId);

        if (context != null && isExpired(context)) {
            log.info("会话已过期: {}", sessionId);
            remove(sessionId);
            return null;
        }

        return context;
    }

    /**
     * 创建新会话
     */
    public ConversationContext create(String sessionId, String userId) {
        log.info("创建新会话: sessionId={}, userId={}", sessionId, userId);

        ConversationContext context = new ConversationContext();
        context.setSessionId(sessionId);
        context.setUserId(userId);
        context.setTraceId(generateTraceId());

        localCache.put(sessionId, context);

        return context;
    }

    /**
     * 保存会话
     */
    public void save(ConversationContext context) {
        if (context == null || context.getSessionId() == null) {
            return;
        }
        context.setLastActiveTime(LocalDateTime.now());
        localCache.put(context.getSessionId(), context);
    }

    /**
     * 删除会话
     */
    public void remove(String sessionId) {
        log.info("删除会话: {}", sessionId);
        localCache.remove(sessionId);
    }

    private boolean isExpired(ConversationContext context) {
        if (context.getLastActiveTime() == null) {
            return true;
        }
        LocalDateTime expireTime = context.getLastActiveTime()
                .plusMinutes(SESSION_TIMEOUT_MINUTES);
        return LocalDateTime.now().isAfter(expireTime);
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 清理过期会话
     */
    public void cleanExpiredSessions() {
        localCache.entrySet().removeIf(entry -> isExpired(entry.getValue()));
    }
}
