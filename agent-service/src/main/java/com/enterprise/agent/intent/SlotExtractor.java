package com.enterprise.agent.intent;

import com.enterprise.agent.llm.LLMClient;
import com.enterprise.agent.llm.LLMRequest;
import com.enterprise.agent.llm.LLMResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 槽位抽取器
 * 
 * 职责：
 * - 根据已识别的意图类型，确定需要抽取的槽位列表
 * - 调用 LLM 从用户输入中抽取槽位值
 * - 对抽取结果进行格式校验
 * - 明确标记哪些必填槽位缺失
 * 
 * 禁止：
 * - 推断或补全用户未明确提供的信息
 * - 绕过格式校验直接返回 LLM 原始输出
 * - 执行任何业务操作
 * - 接受不在预定义列表中的槽位名
 */
@Component
public class SlotExtractor {

    private static final Logger log = LoggerFactory.getLogger(SlotExtractor.class);

    private final LLMClient llmClient;

    /**
     * 各意图需要的槽位定义
     */
    private static final Map<IntentType, List<SlotDefinition>> SLOT_DEFINITIONS = new HashMap<>();

    static {
        // 查询订单状态需要的参数
        SLOT_DEFINITIONS.put(IntentType.QUERY_ORDER_STATUS, Arrays.asList(
                new SlotDefinition("order_id", "订单编号", true, "ORD-\\d{4}-\\d{6}")));

        // 查询物流信息需要的参数
        SLOT_DEFINITIONS.put(IntentType.QUERY_LOGISTICS, Arrays.asList(
                new SlotDefinition("order_id", "订单编号", true, "ORD-\\d{4}-\\d{6}")));

        // 取消订单需要的参数
        SLOT_DEFINITIONS.put(IntentType.CANCEL_ORDER, Arrays.asList(
                new SlotDefinition("order_id", "订单编号", true, "ORD-\\d{4}-\\d{6}"),
                new SlotDefinition("reason", "取消原因", false, null)));

        // 申请退款需要的参数
        SLOT_DEFINITIONS.put(IntentType.REQUEST_REFUND, Arrays.asList(
                new SlotDefinition("order_id", "订单编号", true, "ORD-\\d{4}-\\d{6}"),
                new SlotDefinition("refund_reason", "退款原因", true, null)));

        // 投诉需要的参数
        SLOT_DEFINITIONS.put(IntentType.COMPLAINT, Arrays.asList(
                new SlotDefinition("order_id", "订单编号", false, "ORD-\\d{4}-\\d{6}"),
                new SlotDefinition("complaint_content", "投诉内容", true, null)));

        // 查询账户信息
        SLOT_DEFINITIONS.put(IntentType.QUERY_ACCOUNT, Collections.emptyList());
    }

    @Autowired
    public SlotExtractor(LLMClient llmClient) {
        this.llmClient = llmClient;
    }

    /**
     * 抽取槽位
     */
    public IntentResult extractSlots(IntentResult intentResult) {
        IntentType intentType = intentResult.getIntentType();
        String userMessage = intentResult.getRawInput();

        List<SlotDefinition> slotDefs = SLOT_DEFINITIONS.get(intentType);
        if (slotDefs == null || slotDefs.isEmpty()) {
            // 该意图不需要槽位
            intentResult.setSlots(Collections.emptyMap());
            intentResult.setMissingSlots(Collections.emptyList());
            return intentResult;
        }

        try {
            // 1. 构建 Prompt
            String prompt = buildSlotPrompt(intentType, slotDefs, userMessage);

            // 2. 调用 LLM
            LLMRequest request = LLMRequest.builder()
                    .prompt(prompt)
                    .maxTokens(300)
                    .temperature(0.1)
                    .build();

            LLMResponse response = llmClient.complete(request);

            if (!response.isSuccess()) {
                log.error("槽位抽取 LLM 调用失败: {}", response.getErrorMessage());
                intentResult.setMissingSlots(getRequiredSlotNames(slotDefs));
                return intentResult;
            }

            // 3. 解析并校验
            Map<String, Object> slots = parseAndValidateSlots(response.getContent(), slotDefs);
            List<String> missingSlots = findMissingRequiredSlots(slots, slotDefs);

            intentResult.setSlots(slots);
            intentResult.setMissingSlots(missingSlots);

            return intentResult;

        } catch (Exception e) {
            log.error("槽位抽取异常: {}", e.getMessage(), e);
            intentResult.setMissingSlots(getRequiredSlotNames(slotDefs));
            return intentResult;
        }
    }

    /**
     * 构建槽位抽取 Prompt
     */
    private String buildSlotPrompt(IntentType intentType, List<SlotDefinition> slotDefs, String userMessage) {
        StringBuilder slotDesc = new StringBuilder();
        for (SlotDefinition def : slotDefs) {
            slotDesc.append("- ").append(def.getName())
                    .append(": ").append(def.getDescription())
                    .append(" (").append(def.isRequired() ? "必填" : "选填").append(")\n");
        }

        return "你是一个参数抽取专家。请从用户输入中提取以下参数。\n\n" +
                "【意图】：" + intentType.getDisplayName() + "\n\n" +
                "【需要提取的参数】：\n" + slotDesc + "\n" +
                "【用户输入】：\n" + userMessage + "\n\n" +
                "【输出要求】：\n" +
                "1. 以严格的 JSON 格式返回\n" +
                "2. 只提取用户明确提到的信息\n" +
                "3. 无法提取的参数值设为 null\n" +
                "4. 不要推断或补全任何信息\n\n" +
                "示例输出：{\"order_id\": \"ORD-2026-001234\", \"reason\": null}";
    }

    /**
     * 解析并校验槽位
     */
    private Map<String, Object> parseAndValidateSlots(String llmResponse, List<SlotDefinition> slotDefs) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 提取 JSON
            String jsonStr = extractJson(llmResponse);

            for (SlotDefinition def : slotDefs) {
                String value = extractJsonValue(jsonStr, def.getName());

                // 空值处理
                if (value.isEmpty() || "null".equalsIgnoreCase(value)) {
                    result.put(def.getName(), null);
                    continue;
                }

                // 格式校验
                if (def.getPattern() != null && !value.matches(def.getPattern())) {
                    log.warn("槽位 {} 格式校验失败: {}", def.getName(), value);
                    result.put(def.getName(), null);
                    continue;
                }

                result.put(def.getName(), value);
            }

        } catch (Exception e) {
            log.warn("解析槽位响应失败: {}", e.getMessage());
        }

        return result;
    }

    /**
     * 查找缺失的必填槽位
     */
    private List<String> findMissingRequiredSlots(Map<String, Object> slots, List<SlotDefinition> slotDefs) {
        List<String> missing = new ArrayList<>();
        for (SlotDefinition def : slotDefs) {
            if (def.isRequired() && slots.get(def.getName()) == null) {
                missing.add(def.getName());
            }
        }
        return missing;
    }

    /**
     * 获取所有必填槽位名称
     */
    private List<String> getRequiredSlotNames(List<SlotDefinition> slotDefs) {
        List<String> required = new ArrayList<>();
        for (SlotDefinition def : slotDefs) {
            if (def.isRequired()) {
                required.add(def.getName());
            }
        }
        return required;
    }

    private String extractJson(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }

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

    /**
     * 槽位定义
     */
    public static class SlotDefinition {
        private final String name;
        private final String description;
        private final boolean required;
        private final String pattern;

        public SlotDefinition(String name, String description, boolean required, String pattern) {
            this.name = name;
            this.description = description;
            this.required = required;
            this.pattern = pattern;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public boolean isRequired() {
            return required;
        }

        public String getPattern() {
            return pattern;
        }
    }
}
