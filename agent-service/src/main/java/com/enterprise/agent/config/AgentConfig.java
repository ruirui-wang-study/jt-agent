package com.enterprise.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Agent 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "agent")
public class AgentConfig {

    /**
     * 意图识别置信度阈值
     */
    private double intentConfidenceThreshold = 0.7;

    /**
     * RAG 相似度阈值
     */
    private double ragSimilarityThreshold = 0.7;

    /**
     * 最大对话轮数
     */
    private int maxConversationRounds = 20;

    /**
     * 会话超时时间（分钟）
     */
    private int sessionTimeoutMinutes = 30;

    /**
     * 未识别意图最大重试次数
     */
    private int maxUnknownIntentRetries = 3;

    /**
     * 是否启用调试模式
     */
    private boolean debugMode = false;
}
