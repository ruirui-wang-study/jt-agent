package com.enterprise.agent.log;

import com.enterprise.agent.controller.dto.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Agent 专用日志记录器
 */
@Component
public class AgentLogger {

    private static final Logger log = LoggerFactory.getLogger(AgentLogger.class);
    private static final String LOG_PREFIX = "[AGENT]";

    /**
     * 记录请求日志
     */
    public void logRequest(String traceId, String sessionId, String userId, String message) {
        log.info("{} [{}] REQUEST: sessionId={}, userId={}, message={}",
                LOG_PREFIX,
                traceId,
                sessionId,
                userId,
                truncate(message, 100));
    }

    /**
     * 记录响应日志
     */
    public void logResponse(String traceId, ChatResponse response) {
        log.info("{} [{}] RESPONSE: code={}, state={}, needHandoff={}, reply={}",
                LOG_PREFIX,
                traceId,
                response.getCode(),
                response.getState(),
                response.isNeedHumanHandoff(),
                truncate(response.getReply(), 100));
    }

    /**
     * 记录错误日志
     */
    public void logError(String traceId, Exception e) {
        log.error("{} [{}] ERROR: type={}, message={}",
                LOG_PREFIX,
                traceId,
                e.getClass().getSimpleName(),
                e.getMessage());
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
