package com.enterprise.agent.controller.dto;

import java.io.Serializable;
import java.util.Map;

/**
 * Agent 响应 DTO
 */
public class ChatResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应状态码
     */
    private int code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * Agent 回复内容
     */
    private String reply;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 当前对话状态
     */
    private String state;

    /**
     * 是否需要转人工
     */
    private boolean needHumanHandoff;

    /**
     * 附加数据
     */
    private Map<String, Object> data;

    /**
     * 追踪ID（用于审计）
     */
    private String traceId;

    private ChatResponse() {
    }

    /**
     * 创建成功响应
     */
    public static ChatResponse success(String reply, String sessionId) {
        ChatResponse response = new ChatResponse();
        response.code = 200;
        response.message = "success";
        response.reply = reply;
        response.sessionId = sessionId;
        response.state = "DONE";
        response.needHumanHandoff = false;
        return response;
    }

    /**
     * 创建错误响应
     */
    public static ChatResponse error(int code, String message) {
        ChatResponse response = new ChatResponse();
        response.code = code;
        response.message = message;
        response.needHumanHandoff = false;
        return response;
    }

    /**
     * 创建需要澄清的响应
     */
    public static ChatResponse needClarify(String reply, String sessionId) {
        ChatResponse response = new ChatResponse();
        response.code = 200;
        response.message = "need_clarify";
        response.reply = reply;
        response.sessionId = sessionId;
        response.state = "NEED_CLARIFY";
        response.needHumanHandoff = false;
        return response;
    }

    /**
     * 创建转人工响应
     */
    public static ChatResponse handoff(String reply, String sessionId) {
        ChatResponse response = new ChatResponse();
        response.code = 200;
        response.message = "handoff";
        response.reply = reply;
        response.sessionId = sessionId;
        response.state = "HUMAN_HANDOFF";
        response.needHumanHandoff = true;
        return response;
    }

    // ==================== Getter/Setter ====================

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public boolean isNeedHumanHandoff() {
        return needHumanHandoff;
    }

    public void setNeedHumanHandoff(boolean needHumanHandoff) {
        this.needHumanHandoff = needHumanHandoff;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
