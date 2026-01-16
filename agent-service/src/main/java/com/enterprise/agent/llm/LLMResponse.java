package com.enterprise.agent.llm;

import java.io.Serializable;

/**
 * LLM 响应结构
 */
public class LLMResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 生成的内容
     */
    private String content;

    /**
     * 错误消息
     */
    private String errorMessage;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 使用的 prompt token 数量
     */
    private int promptTokens;

    /**
     * 生成的 token 数量
     */
    private int completionTokens;

    /**
     * 总 token 数量
     */
    private int totalTokens;

    /**
     * 请求耗时（毫秒）
     */
    private long latencyMs;

    /**
     * 模型名称
     */
    private String model;

    private LLMResponse() {
    }

    /**
     * 创建成功响应
     */
    public static LLMResponse success(String content) {
        LLMResponse response = new LLMResponse();
        response.success = true;
        response.content = content;
        return response;
    }

    /**
     * 创建失败响应
     */
    public static LLMResponse fail(String errorCode, String errorMessage) {
        LLMResponse response = new LLMResponse();
        response.success = false;
        response.errorCode = errorCode;
        response.errorMessage = errorMessage;
        return response;
    }

    // ==================== Getter/Setter ====================

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public int getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(int promptTokens) {
        this.promptTokens = promptTokens;
    }

    public int getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(int completionTokens) {
        this.completionTokens = completionTokens;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(int totalTokens) {
        this.totalTokens = totalTokens;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    @Override
    public String toString() {
        return "LLMResponse{" +
                "success=" + success +
                ", contentLength=" + (content != null ? content.length() : 0) +
                ", errorCode='" + errorCode + '\'' +
                ", latencyMs=" + latencyMs +
                ", totalTokens=" + totalTokens +
                '}';
    }
}
