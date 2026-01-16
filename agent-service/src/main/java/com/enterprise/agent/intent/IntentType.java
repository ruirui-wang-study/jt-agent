package com.enterprise.agent.intent;

/**
 * 意图类型枚举（冻结）
 * 
 * 所有意图必须在此枚举中预定义
 * LLM 识别结果必须映射到此枚举，不允许自创类型
 */
public enum IntentType {

    // ==================== 订单相关 ====================

    /**
     * 查询订单状态
     */
    QUERY_ORDER_STATUS("query_order_status", "查询订单状态", false),

    /**
     * 查询物流信息
     */
    QUERY_LOGISTICS("query_logistics", "查询物流信息", false),

    /**
     * 取消订单
     */
    CANCEL_ORDER("cancel_order", "取消订单", true),

    /**
     * 修改订单
     */
    MODIFY_ORDER("modify_order", "修改订单", true),

    // ==================== 售后相关 ====================

    /**
     * 申请退款
     */
    REQUEST_REFUND("request_refund", "申请退款", true),

    /**
     * 投诉
     */
    COMPLAINT("complaint", "投诉", true),

    /**
     * 咨询问题
     */
    CONSULTATION("consultation", "咨询问题", false),

    // ==================== 账户相关 ====================

    /**
     * 查询账户信息
     */
    QUERY_ACCOUNT("query_account", "查询账户信息", false),

    /**
     * 修改账户信息
     */
    MODIFY_ACCOUNT("modify_account", "修改账户信息", true),

    // ==================== 特殊意图 ====================

    /**
     * 用户请求转人工
     */
    HUMAN_HANDOFF("human_handoff", "转人工", false),

    /**
     * 闲聊/打招呼
     */
    GREETING("greeting", "闲聊", false),

    /**
     * 结束对话
     */
    END_CONVERSATION("end_conversation", "结束对话", false),

    /**
     * 无法识别的意图
     */
    UNKNOWN("unknown", "无法识别", false);

    /**
     * 意图代码（用于 LLM 返回映射）
     */
    private final String code;

    /**
     * 显示名称
     */
    private final String displayName;

    /**
     * 是否为高风险操作
     */
    private final boolean highRisk;

    IntentType(String code, String displayName, boolean highRisk) {
        this.code = code;
        this.displayName = displayName;
        this.highRisk = highRisk;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isHighRisk() {
        return highRisk;
    }

    /**
     * 根据 code 查找意图类型
     * 找不到返回 UNKNOWN
     */
    public static IntentType fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return UNKNOWN;
        }
        for (IntentType type : values()) {
            if (type.code.equalsIgnoreCase(code.trim())) {
                return type;
            }
        }
        return UNKNOWN;
    }

    /**
     * 是否需要工具执行
     */
    public boolean needToolExecution() {
        return this != GREETING
                && this != END_CONVERSATION
                && this != UNKNOWN
                && this != HUMAN_HANDOFF;
    }

    /**
     * 获取所有意图代码列表（用于 Prompt）
     */
    public static String getAllCodesForPrompt() {
        StringBuilder sb = new StringBuilder();
        for (IntentType type : values()) {
            if (type != UNKNOWN) {
                sb.append("- ").append(type.code).append(": ").append(type.displayName).append("\n");
            }
        }
        return sb.toString();
    }
}
