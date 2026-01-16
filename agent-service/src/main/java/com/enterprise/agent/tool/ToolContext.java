package com.enterprise.agent.tool;

import java.io.Serializable;

/**
 * 工具执行上下文
 * 
 * 封装工具执行所需的上下文信息
 */
public class ToolContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 追踪ID（用于全链路追踪）
     */
    private String traceId;

    /**
     * 用户权限级别
     */
    private int permissionLevel;

    /**
     * 是否为调试模式
     */
    private boolean debug;

    private ToolContext() {
    }

    public static Builder builder() {
        return new Builder();
    }

    // ==================== Getter ====================

    public String getUserId() {
        return userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getTraceId() {
        return traceId;
    }

    public int getPermissionLevel() {
        return permissionLevel;
    }

    public boolean isDebug() {
        return debug;
    }

    // ==================== Builder ====================

    public static class Builder {
        private final ToolContext context = new ToolContext();

        public Builder userId(String userId) {
            context.userId = userId;
            return this;
        }

        public Builder sessionId(String sessionId) {
            context.sessionId = sessionId;
            return this;
        }

        public Builder traceId(String traceId) {
            context.traceId = traceId;
            return this;
        }

        public Builder permissionLevel(int permissionLevel) {
            context.permissionLevel = permissionLevel;
            return this;
        }

        public Builder debug(boolean debug) {
            context.debug = debug;
            return this;
        }

        public ToolContext build() {
            return context;
        }
    }
}
