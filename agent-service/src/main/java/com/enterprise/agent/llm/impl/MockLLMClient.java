package com.enterprise.agent.llm.impl;

import com.enterprise.agent.llm.LLMClient;
import com.enterprise.agent.llm.LLMRequest;
import com.enterprise.agent.llm.LLMResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock LLM 客户端（用于测试）
 * 
 * 实际项目中应替换为 DeepSeekClient 或 OpenAIClient
 */
@Component
public class MockLLMClient implements LLMClient {

    private static final Logger log = LoggerFactory.getLogger(MockLLMClient.class);

    @Override
    public LLMResponse complete(LLMRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // 模拟 LLM 响应
            String content = generateMockResponse(request.getPrompt());

            LLMResponse response = LLMResponse.success(content);
            response.setLatencyMs(System.currentTimeMillis() - startTime);
            response.setModel("mock-model");
            response.setPromptTokens(request.getPrompt().length() / 4);
            response.setCompletionTokens(content.length() / 4);
            response.setTotalTokens(response.getPromptTokens() + response.getCompletionTokens());

            log.debug("Mock LLM 响应: {}", content);
            return response;

        } catch (Exception e) {
            log.error("Mock LLM 异常: {}", e.getMessage(), e);
            return LLMResponse.fail("MOCK_ERROR", e.getMessage());
        }
    }

    @Override
    public LLMResponse chat(LLMRequest request) {
        return complete(request);
    }

    @Override
    public String getModelName() {
        return "mock-model";
    }

    @Override
    public boolean isHealthy() {
        return true;
    }

    /**
     * 生成模拟响应
     */
    private String generateMockResponse(String prompt) {
        // 意图识别响应
        if (prompt.contains("意图识别")) {
            if (prompt.contains("订单") && prompt.contains("状态")) {
                return "{\"intent\": \"query_order_status\", \"confidence\": 0.95}";
            }
            if (prompt.contains("物流")) {
                return "{\"intent\": \"query_logistics\", \"confidence\": 0.92}";
            }
            if (prompt.contains("转人工") || prompt.contains("人工客服")) {
                return "{\"intent\": \"human_handoff\", \"confidence\": 0.98}";
            }
            if (prompt.contains("你好") || prompt.contains("在吗")) {
                return "{\"intent\": \"greeting\", \"confidence\": 0.90}";
            }
            return "{\"intent\": \"unknown\", \"confidence\": 0.3}";
        }

        // 槽位抽取响应
        if (prompt.contains("参数抽取")) {
            // 尝试从 prompt 中提取订单号
            if (prompt.contains("ORD-")) {
                int start = prompt.indexOf("ORD-");
                int end = Math.min(start + 15, prompt.length());
                String orderId = prompt.substring(start, end).replaceAll("[^A-Z0-9\\-]", "");
                return "{\"order_id\": \"" + orderId + "\", \"reason\": null}";
            }
            return "{\"order_id\": null, \"reason\": null}";
        }

        // 响应生成
        if (prompt.contains("生成自然语言回复")) {
            return "您好！根据您的查询，订单状态为已发货。如您还有其他问题，请随时询问。";
        }

        return "这是一个模拟的 LLM 响应。";
    }
}
