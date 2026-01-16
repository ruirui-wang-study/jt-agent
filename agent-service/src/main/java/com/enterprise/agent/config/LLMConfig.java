package com.enterprise.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * LLM 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "agent.llm")
public class LLMConfig {

    /**
     * API URL
     */
    private String apiUrl = "https://api.deepseek.com/v1/chat/completions";

    /**
     * API Key
     */
    private String apiKey;

    /**
     * 模型名称
     */
    private String modelName = "deepseek-chat";

    /**
     * 超时时间（毫秒）
     */
    private int timeout = 30000;

    /**
     * 最大重试次数
     */
    private int maxRetries = 3;
}
