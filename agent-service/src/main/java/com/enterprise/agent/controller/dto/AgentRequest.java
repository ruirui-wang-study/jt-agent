package com.enterprise.agent.controller.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * Agent 请求 DTO
 */
@Data
public class AgentRequest {

    /**
     * 会话ID（用于多轮对话）
     */
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    /**
     * 用户ID（用于权限校验）
     */
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    /**
     * 用户消息
     */
    @NotBlank(message = "消息内容不能为空")
    private String message;

    /**
     * 额外参数（可选）
     */
    private java.util.Map<String, Object> extra;
}
