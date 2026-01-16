package com.enterprise.agent.response;

import com.enterprise.agent.intent.IntentResult;
import com.enterprise.agent.llm.LLMClient;
import com.enterprise.agent.llm.LLMRequest;
import com.enterprise.agent.llm.LLMResponse;
import com.enterprise.agent.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 响应生成器
 * 
 * 职责：
 * - 将工具返回的结构化数据转换为自然语言回复
 * - 使用 LLM 生成友好的回复
 * - 在 LLM 失败时使用模板兜底
 */
@Component
public class ResponseGenerator {

    private static final Logger log = LoggerFactory.getLogger(ResponseGenerator.class);

    private final LLMClient llmClient;

    @Autowired
    public ResponseGenerator(LLMClient llmClient) {
        this.llmClient = llmClient;
    }

    /**
     * 生成响应
     */
    public String generate(IntentResult intentResult, ToolResult toolResult) {
        try {
            // 1. 构建 Prompt
            String prompt = buildResponsePrompt(intentResult, toolResult);

            // 2. 调用 LLM
            LLMRequest request = LLMRequest.builder()
                    .prompt(prompt)
                    .maxTokens(500)
                    .temperature(0.3)
                    .build();

            LLMResponse response = llmClient.complete(request);

            if (response.isSuccess() && response.getContent() != null) {
                return response.getContent();
            }

            // 3. LLM 失败时使用模板兜底
            log.warn("LLM 生成失败, 使用模板兜底");
            return generateTemplateResponse(intentResult, toolResult);

        } catch (Exception e) {
            log.error("响应生成异常: {}", e.getMessage(), e);
            return generateTemplateResponse(intentResult, toolResult);
        }
    }

    /**
     * 构建响应生成 Prompt
     */
    private String buildResponsePrompt(IntentResult intentResult, ToolResult toolResult) {
        String dataStr = formatToolResult(toolResult);

        return "请根据以下结构化数据，生成自然语言回复。\n\n" +
                "【用户意图】：" + intentResult.getIntentType().getDisplayName() + "\n\n" +
                "【查询结果】：\n" + dataStr + "\n\n" +
                "【要求】：\n" +
                "1. 语气专业友好\n" +
                "2. 只使用上述数据，不要编造任何信息\n" +
                "3. 如果数据中有时间，请用自然的方式表达\n" +
                "4. 结尾询问用户是否还有其他问题";
    }

    /**
     * 格式化工具结果
     */
    private String formatToolResult(ToolResult toolResult) {
        if (toolResult.getData() == null || toolResult.getData().isEmpty()) {
            return "无数据";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : toolResult.getData().entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 模板兜底响应
     */
    private String generateTemplateResponse(IntentResult intentResult, ToolResult toolResult) {
        Map<String, Object> data = toolResult.getData();
        if (data == null || data.isEmpty()) {
            return "已为您查询到相关信息，请问还有其他问题吗？";
        }

        StringBuilder sb = new StringBuilder("查询结果如下：\n");
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // 格式化字段名
            String displayKey = formatFieldName(key);
            sb.append(displayKey).append("：").append(value).append("\n");
        }
        sb.append("\n请问还有其他问题吗？");
        return sb.toString();
    }

    /**
     * 格式化字段名（将数据库字段名转为用户友好名称）
     */
    private String formatFieldName(String fieldName) {
        switch (fieldName.toUpperCase()) {
            case "ORDER_ID":
                return "订单编号";
            case "ORDER_STATUS":
                return "订单状态";
            case "ORDER_AMOUNT":
                return "订单金额";
            case "CREATE_TIME":
                return "创建时间";
            case "UPDATE_TIME":
                return "更新时间";
            case "RECEIVER_NAME":
                return "收货人";
            case "RECEIVER_PHONE":
                return "联系电话";
            case "RECEIVER_ADDRESS":
                return "收货地址";
            case "PRODUCT_NAME":
                return "商品信息";
            case "LOGISTICS_STATUS":
                return "物流状态";
            case "CURRENT_LOCATION":
                return "当前位置";
            case "ESTIMATE_ARRIVAL":
                return "预计送达";
            default:
                return fieldName;
        }
    }
}
