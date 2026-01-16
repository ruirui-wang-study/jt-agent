package com.enterprise.agent.controller.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Agent 响应 DTO
 */
@Data
@Builder
public class AgentResponse {

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

    public static AgentResponse success(String reply, String sessionId) {
        return AgentResponse.builder()
                .code(200)
                .message("success")
                .reply(reply)
                .sessionId(sessionId)
                .needHumanHandoff(false)
                .build();
    }

    public static AgentResponse error(int code, String message) {
        return AgentResponse.builder()
                .code(code)
                .message(message)
                .needHumanHandoff(false)
                .build();
    }

    public static AgentResponse needClarify(String reply, String sessionId) {
        return AgentResponse.builder()
                .code(200)
                .message("need_clarify")
                .reply(reply)
                .sessionId(sessionId)
                .state("NEED_CLARIFY")
                .needHumanHandoff(false)
                .build();
    }

    public static AgentResponse handoff(String reply, String sessionId) {
        return AgentResponse.builder()
                .code(200)
                .message("handoff")
                .reply(reply)
                .sessionId(sessionId)
                .state("HUMAN_HANDOFF")
                .needHumanHandoff(true)
                .build();
    }
}
