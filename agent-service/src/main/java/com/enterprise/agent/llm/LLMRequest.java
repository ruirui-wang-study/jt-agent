package com.enterprise.agent.llm;

import java.io.Serializable;
import java.util.List;

/**
 * LLM 请求结构
 */
public class LLMRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 提示词（通过模板生成，非原始用户输入）
     */
    private String prompt;

    /**
     * 多轮对话消息历史
     */
    private List<Message> messages;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 最大生成 token 数
     */
    private int maxTokens = 1024;

    /**
     * 温度参数 (0.0 - 2.0)
     */
    private double temperature = 0.7;

    /**
     * Top-P 采样参数
     */
    private double topP = 0.9;

    /**
     * 停止词列表
     */
    private List<String> stopWords;

    private LLMRequest() {
    }

    public static Builder builder() {
        return new Builder();
    }

    // ==================== Getter ====================

    public String getPrompt() {
        return prompt;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getTopP() {
        return topP;
    }

    public List<String> getStopWords() {
        return stopWords;
    }

    // ==================== Message ====================

    public static class Message implements Serializable {
        private static final long serialVersionUID = 1L;

        private String role;
        private String content;

        public Message() {
        }

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    // ==================== Builder ====================

    public static class Builder {
        private final LLMRequest request = new LLMRequest();

        public Builder prompt(String prompt) {
            request.prompt = prompt;
            return this;
        }

        public Builder messages(List<Message> messages) {
            request.messages = messages;
            return this;
        }

        public Builder systemPrompt(String systemPrompt) {
            request.systemPrompt = systemPrompt;
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            request.maxTokens = maxTokens;
            return this;
        }

        public Builder temperature(double temperature) {
            request.temperature = temperature;
            return this;
        }

        public Builder topP(double topP) {
            request.topP = topP;
            return this;
        }

        public Builder stopWords(List<String> stopWords) {
            request.stopWords = stopWords;
            return this;
        }

        public LLMRequest build() {
            return request;
        }
    }
}
