package com.enterprise.agent.controller.dto;

import java.io.Serializable;
import java.util.Map;

/**
 * Agent 请求 DTO
 */
public class ChatRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话ID（用于多轮对话）
     */
    private String sessionId;

    /**
     * 用户ID（用于权限校验）
     */
    private String userId;

    /**
     * 用户消息
     */
    private String message;

    /**
     * 额外参数（可选）
     */
    private Map<String, Object> extra;

    // ==================== Getter/Setter ====================

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }

    public void setExtra(Map<String, Object> extra) {
        this.extra = extra;
    }

    /**
     * 校验必填字段
     */
    public String validate() {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return "sessionId 不能为空";
        }
        if (userId == null || userId.trim().isEmpty()) {
            return "userId 不能为空";
        }
        if (message == null || message.trim().isEmpty()) {
            return "message 不能为空";
        }
        if (message.length() > 2000) {
            return "message 长度不能超过 2000 字符";
        }
        return null;
    }
}
