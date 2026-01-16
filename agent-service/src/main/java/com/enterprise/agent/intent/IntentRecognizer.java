package com.enterprise.agent.intent;

import com.enterprise.agent.llm.LLMClient;
import com.enterprise.agent.llm.LLMRequest;
import com.enterprise.agent.llm.LLMResponse;
import com.enterprise.agent.session.ConversationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 意图识别器
 * 
 * 职责：
 * - 调用 LLM 将用户自然语言输入转换为预定义的意图类型
 * - 返回意图类型（IntentType 枚举）和置信度分数
 * - 提供对话历史摘要给 LLM 作为上下文参考
 * 
 * 禁止：
 * - 接受 LLM 返回的非预定义意图类型
 * - 在此模块内执行任何业务操作
 * - 直接修改会话状态
 * - 将用户原始输入未经结构化处理直接拼入 Prompt
 */
@Component
public class IntentRecognizer {

    private static final Logger log = LoggerFactory.getLogger(IntentRecognizer.class);

    private final LLMClient llmClient;

    @Autowired
    public IntentRecognizer(LLMClient llmClient) {
        this.llmClient = llmClient;
    }

    /**
     * 识别用户意图
     */
    public IntentResult recognize(ConversationContext context, String userMessage) {
        String traceId = context.getTraceId();

        try {
            // 1. 构建 Prompt
            String prompt = buildIntentPrompt(context, userMessage);

            // 2. 调用 LLM
            LLMRequest request = LLMRequest.builder()
                    .prompt(prompt)
                    .maxTokens(200)
                    .temperature(0.1) // 低温度以获得更确定的结果
                    .build();

            LLMResponse response = llmClient.complete(request);

            if (!response.isSuccess()) {
                log.error("[{}] LLM 调用失败: {}", traceId, response.getErrorMessage());
                return IntentResult.failed(userMessage, response.getErrorMessage());
            }

            // 3. 解析 LLM 响应
            return parseIntentResponse(response.getContent(), userMessage);

        } catch (Exception e) {
            log.error("[{}] 意图识别异常: {}", traceId, e.getMessage(), e);
            return IntentResult.failed(userMessage, e.getMessage());
        }
    }

    /**
     * 构建意图识别 Prompt
     */
    private String buildIntentPrompt(ConversationContext context, String userMessage) {
        // 获取所有意图类型列表
        String intentList = IntentType.getAllCodesForPrompt();

        // 构建历史对话摘要（最近3轮）
        String historyBrief = context.getRecentHistoryBrief(3);

        return "你是一个意图识别专家。请根据用户输入，识别其意图。\n\n" +
                "【可选意图列表】（只能从以下选项中选择）：\n" +
                intentList + "\n" +
                "【历史对话摘要】：\n" +
                historyBrief + "\n" +
                "【当前用户输入】：\n" +
                userMessage + "\n\n" +
                "【输出要求】：\n" +
                "请以严格的 JSON 格式返回，不要包含任何其他内容：\n" +
                "{\"intent\": \"意图代码\", \"confidence\": 0.xx}\n\n" +
                "注意：\n" +
                "1. intent 必须是上述意图列表中的 code\n" +
                "2. 如果无法确定，返回 {\"intent\": \"unknown\", \"confidence\": 0.0}\n" +
                "3. confidence 范围 0.0-1.0";
    }

    /**
     * 解析 LLM 响应
     */
    private IntentResult parseIntentResponse(String llmResponse, String rawInput) {
        try {
            // 提取 JSON 部分
            String jsonStr = extractJson(llmResponse);

            // 简单的 JSON 解析（避免引入额外依赖）
            String intentCode = extractJsonValue(jsonStr, "intent");
            String confidenceStr = extractJsonValue(jsonStr, "confidence");

            double confidence = 0.0;
            try {
                confidence = Double.parseDouble(confidenceStr);
            } catch (NumberFormatException e) {
                log.warn("解析置信度失败: {}", confidenceStr);
            }

            IntentType intentType = IntentType.fromCode(intentCode);

            return IntentResult.builder()
                    .intentType(intentType)
                    .confidence(confidence)
                    .rawInput(rawInput)
                    .llmRawResponse(llmResponse)
                    .build();

        } catch (Exception e) {
            log.warn("解析意图响应失败: {}", e.getMessage());
            return IntentResult.failed(rawInput, "解析失败: " + llmResponse);
        }
    }

    /**
     * 从 LLM 响应中提取 JSON
     */
    private String extractJson(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }

    /**
     * 简单的 JSON 值提取
     */
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex < 0) {
            return "";
        }

        int colonIndex = json.indexOf(':', keyIndex);
        if (colonIndex < 0) {
            return "";
        }

        int valueStart = colonIndex + 1;
        while (valueStart < json.length() &&
                (json.charAt(valueStart) == ' ' || json.charAt(valueStart) == '"')) {
            valueStart++;
        }

        int valueEnd = valueStart;
        while (valueEnd < json.length()) {
            char c = json.charAt(valueEnd);
            if (c == '"' || c == ',' || c == '}') {
                break;
            }
            valueEnd++;
        }

        return json.substring(valueStart, valueEnd).trim();
    }
}
