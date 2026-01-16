package com.enterprise.agent.intent;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 意图识别结果
 * 
 * 封装意图识别和槽位抽取的完整结果
 */
public class IntentResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 识别出的意图类型（必须为预定义枚举值）
     */
    private IntentType intentType;

    /**
     * 置信度分数 (0.0 - 1.0)
     */
    private double confidence;

    /**
     * 抽取的槽位参数
     */
    private Map<String, Object> slots;

    /**
     * 缺失的必填槽位列表
     */
    private List<String> missingSlots;

    /**
     * 用户原始输入（用于追溯）
     */
    private String rawInput;

    /**
     * LLM 返回的原始响应（用于调试和审计）
     */
    private String llmRawResponse;

    /**
     * 私有构造函数，使用 Builder 创建实例
     */
    private IntentResult() {
    }

    /**
     * 判断意图是否识别成功
     * 条件：意图类型非 UNKNOWN 且置信度达标
     */
    public boolean isRecognized() {
        return intentType != null
                && intentType != IntentType.UNKNOWN
                && confidence >= 0.7;
    }

    /**
     * 判断所有必填槽位是否已填充
     */
    public boolean isSlotsComplete() {
        return missingSlots == null || missingSlots.isEmpty();
    }

    /**
     * 创建识别失败的结果
     */
    public static IntentResult failed(String rawInput, String reason) {
        IntentResult result = new IntentResult();
        result.intentType = IntentType.UNKNOWN;
        result.confidence = 0.0;
        result.rawInput = rawInput;
        result.llmRawResponse = reason;
        result.slots = Collections.emptyMap();
        result.missingSlots = Collections.emptyList();
        return result;
    }

    /**
     * 创建 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    // ==================== Getter/Setter ====================

    public IntentType getIntentType() {
        return intentType;
    }

    public void setIntentType(IntentType intentType) {
        this.intentType = intentType;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public Map<String, Object> getSlots() {
        return slots;
    }

    public void setSlots(Map<String, Object> slots) {
        this.slots = slots;
    }

    public List<String> getMissingSlots() {
        return missingSlots;
    }

    public void setMissingSlots(List<String> missingSlots) {
        this.missingSlots = missingSlots;
    }

    public String getRawInput() {
        return rawInput;
    }

    public void setRawInput(String rawInput) {
        this.rawInput = rawInput;
    }

    public String getLlmRawResponse() {
        return llmRawResponse;
    }

    public void setLlmRawResponse(String llmRawResponse) {
        this.llmRawResponse = llmRawResponse;
    }

    @Override
    public String toString() {
        return "IntentResult{" +
                "intentType=" + intentType +
                ", confidence=" + confidence +
                ", slots=" + slots +
                ", missingSlots=" + missingSlots +
                ", rawInput='" + rawInput + '\'' +
                '}';
    }

    // ==================== Builder ====================

    public static class Builder {
        private final IntentResult result = new IntentResult();

        public Builder intentType(IntentType intentType) {
            result.intentType = intentType;
            return this;
        }

        public Builder confidence(double confidence) {
            result.confidence = confidence;
            return this;
        }

        public Builder slots(Map<String, Object> slots) {
            result.slots = slots;
            return this;
        }

        public Builder missingSlots(List<String> missingSlots) {
            result.missingSlots = missingSlots;
            return this;
        }

        public Builder rawInput(String rawInput) {
            result.rawInput = rawInput;
            return this;
        }

        public Builder llmRawResponse(String llmRawResponse) {
            result.llmRawResponse = llmRawResponse;
            return this;
        }

        public IntentResult build() {
            if (result.slots == null) {
                result.slots = Collections.emptyMap();
            }
            if (result.missingSlots == null) {
                result.missingSlots = Collections.emptyList();
            }
            return result;
        }
    }
}
