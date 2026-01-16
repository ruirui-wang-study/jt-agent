package com.enterprise.agent.orchestrator;

/**
 * Agent 对话状态枚举
 * 
 * 定义状态机的所有状态
 */
public enum AgentState {

    /**
     * 初始状态
     */
    INIT("初始状态"),

    /**
     * 意图识别中
     */
    INTENT_RECOGNITION("意图识别中"),

    /**
     * 槽位抽取中
     */
    SLOT_EXTRACTION("槽位抽取中"),

    /**
     * 槽位完整，准备执行工具
     */
    SLOT_COMPLETE("槽位完整"),

    /**
     * 需要澄清（参数缺失或不明确）
     */
    NEED_CLARIFY("需要澄清"),

    /**
     * 未知意图
     */
    UNKNOWN_INTENT("未知意图"),

    /**
     * 禁止访问
     */
    FORBIDDEN("禁止访问"),

    /**
     * 工具执行中
     */
    TOOL_EXECUTION("工具执行中"),

    /**
     * 响应生成中
     */
    RESPONSE_GENERATION("响应生成中"),

    /**
     * 询问用户（等待用户补充信息）
     */
    ASK_USER("询问用户"),

    /**
     * 兜底处理
     */
    FALLBACK("兜底处理"),

    /**
     * 转人工
     */
    HUMAN_HANDOFF("转人工"),

    /**
     * 拒绝回答
     */
    REJECT("拒绝回答"),

    /**
     * 完成
     */
    DONE("完成"),

    /**
     * 错误
     */
    ERROR("错误");

    private final String displayName;

    AgentState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
