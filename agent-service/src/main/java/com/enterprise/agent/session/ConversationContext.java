package com.enterprise.agent.session;

import com.enterprise.agent.intent.IntentResult;
import com.enterprise.agent.orchestrator.AgentState;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 对话上下文
 * 
 * 维护单次会话的所有状态信息
 */
public class ConversationContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 追踪ID
     */
    private String traceId;

    /**
     * 当前状态
     */
    private AgentState state = AgentState.INIT;

    /**
     * 用户权限级别
     */
    private int permissionLevel = 1;

    /**
     * 对话历史
     */
    private List<Message> history = new ArrayList<>();

    /**
     * 状态转换历史
     */
    private List<StateTransition> stateHistory = new ArrayList<>();

    /**
     * 待处理的意图（用于多轮补充信息）
     */
    private IntentResult pendingIntent;

    /**
     * 未识别意图计数
     */
    private int unknownIntentCount = 0;

    /**
     * 会话创建时间
     */
    private LocalDateTime createTime;

    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveTime;

    public ConversationContext() {
        this.createTime = LocalDateTime.now();
        this.lastActiveTime = LocalDateTime.now();
    }

    /**
     * 添加用户消息
     */
    public void addUserMessage(String content) {
        history.add(new Message("user", content, LocalDateTime.now()));
        lastActiveTime = LocalDateTime.now();
    }

    /**
     * 添加助手消息
     */
    public void addAssistantMessage(String content) {
        history.add(new Message("assistant", content, LocalDateTime.now()));
    }

    /**
     * 获取最近的历史摘要（用于 LLM 上下文）
     */
    public String getRecentHistoryBrief(int rounds) {
        if (history.isEmpty()) {
            return "无历史对话";
        }

        int start = Math.max(0, history.size() - rounds * 2);
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < history.size(); i++) {
            Message msg = history.get(i);
            sb.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 添加状态转换记录
     */
    public void addStateHistory(AgentState from, AgentState to) {
        stateHistory.add(new StateTransition(from, to, LocalDateTime.now()));
    }

    /**
     * 清空状态历史
     */
    public void clearStateHistory() {
        stateHistory.clear();
    }

    /**
     * 增加未识别意图计数
     */
    public int incrementUnknownIntentCount() {
        return ++unknownIntentCount;
    }

    /**
     * 重置未识别意图计数
     */
    public void resetUnknownIntentCount() {
        unknownIntentCount = 0;
    }

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

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public AgentState getState() {
        return state;
    }

    public void setState(AgentState state) {
        this.state = state;
    }

    public int getPermissionLevel() {
        return permissionLevel;
    }

    public void setPermissionLevel(int permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    public List<Message> getHistory() {
        return history;
    }

    public void setHistory(List<Message> history) {
        this.history = history;
    }

    public List<StateTransition> getStateHistory() {
        return stateHistory;
    }

    public void setStateHistory(List<StateTransition> stateHistory) {
        this.stateHistory = stateHistory;
    }

    public IntentResult getPendingIntent() {
        return pendingIntent;
    }

    public void setPendingIntent(IntentResult pendingIntent) {
        this.pendingIntent = pendingIntent;
    }

    public int getUnknownIntentCount() {
        return unknownIntentCount;
    }

    public void setUnknownIntentCount(int unknownIntentCount) {
        this.unknownIntentCount = unknownIntentCount;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getLastActiveTime() {
        return lastActiveTime;
    }

    public void setLastActiveTime(LocalDateTime lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }

    // ==================== 内部类 ====================

    /**
     * 消息
     */
    public static class Message implements Serializable {
        private static final long serialVersionUID = 1L;

        private String role;
        private String content;
        private LocalDateTime time;

        public Message() {
        }

        public Message(String role, String content, LocalDateTime time) {
            this.role = role;
            this.content = content;
            this.time = time;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public LocalDateTime getTime() {
            return time;
        }

        public void setTime(LocalDateTime time) {
            this.time = time;
        }
    }

    /**
     * 状态转换记录
     */
    public static class StateTransition implements Serializable {
        private static final long serialVersionUID = 1L;

        private AgentState from;
        private AgentState to;
        private LocalDateTime time;

        public StateTransition() {
        }

        public StateTransition(AgentState from, AgentState to, LocalDateTime time) {
            this.from = from;
            this.to = to;
            this.time = time;
        }

        public AgentState getFrom() {
            return from;
        }

        public void setFrom(AgentState from) {
            this.from = from;
        }

        public AgentState getTo() {
            return to;
        }

        public void setTo(AgentState to) {
            this.to = to;
        }

        public LocalDateTime getTime() {
            return time;
        }

        public void setTime(LocalDateTime time) {
            this.time = time;
        }
    }
}
