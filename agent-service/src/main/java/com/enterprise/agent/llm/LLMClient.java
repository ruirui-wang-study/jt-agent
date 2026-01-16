package com.enterprise.agent.llm;

/**
 * LLM 客户端接口
 * 
 * 抽象大模型调用，支持对接不同的 LLM 服务提供商
 * 
 * 职责：
 * - 提供统一的 LLM 调用接口
 * - 管理 Prompt 模板
 * - 记录调用日志
 * 
 * 禁止：
 * - 将用户原始输入未经模板处理直接作为 prompt
 * - 在 prompt 中包含敏感信息
 * - 让 LLM 返回的内容不经校验直接作为最终响应
 * - 在此模块内做业务逻辑判断
 */
public interface LLMClient {

    /**
     * 文本生成（单轮对话，使用 Prompt 模板）
     *
     * @param request 请求参数
     * @return 生成结果
     */
    LLMResponse complete(LLMRequest request);

    /**
     * 多轮对话
     *
     * @param request 请求参数（包含消息历史）
     * @return 生成结果
     */
    LLMResponse chat(LLMRequest request);

    /**
     * 获取模型名称
     * 
     * @return 模型名称
     */
    String getModelName();

    /**
     * 健康检查
     * 
     * @return 服务是否可用
     */
    boolean isHealthy();

    /**
     * 获取降级回复（LLM 不可用时的兜底）
     * 
     * @return 降级回复内容
     */
    default String getFallbackResponse() {
        return "抱歉，系统暂时无法处理您的请求，请稍后重试或联系人工客服。";
    }
}
