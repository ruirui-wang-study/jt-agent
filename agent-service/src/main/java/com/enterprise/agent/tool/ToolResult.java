package com.enterprise.agent.tool;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * 工具执行结果
 * 
 * 所有 Tool 执行后返回此统一结构
 */
public class ToolResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否执行成功
     */
    private boolean success;

    /**
     * 结果数据（结构化）
     */
    private Map<String, Object> data;

    /**
     * 错误码（失败时）
     */
    private String errorCode;

    /**
     * 错误消息（失败时）
     */
    private String errorMessage;

    /**
     * 执行耗时（毫秒）
     */
    private long executeTimeMs;

    /**
     * 数据来源（用于审计追踪，如表名/视图名/API名）
     */
    private String dataSource;

    private ToolResult() {
    }

    /**
     * 创建成功结果
     */
    public static ToolResult success(Map<String, Object> data, String dataSource) {
        ToolResult result = new ToolResult();
        result.success = true;
        result.data = data != null ? data : Collections.emptyMap();
        result.dataSource = dataSource;
        return result;
    }

    /**
     * 创建失败结果
     */
    public static ToolResult fail(String errorCode, String errorMessage) {
        ToolResult result = new ToolResult();
        result.success = false;
        result.errorCode = errorCode;
        result.errorMessage = errorMessage;
        result.data = Collections.emptyMap();
        return result;
    }

    /**
     * 创建无数据结果（查询成功但无匹配数据）
     */
    public static ToolResult noData(String message) {
        ToolResult result = new ToolResult();
        result.success = true;
        result.errorMessage = message;
        result.data = Collections.emptyMap();
        return result;
    }

    // ==================== Getter/Setter ====================

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public long getExecuteTimeMs() {
        return executeTimeMs;
    }

    public void setExecuteTimeMs(long executeTimeMs) {
        this.executeTimeMs = executeTimeMs;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 判断是否有数据
     */
    public boolean hasData() {
        return data != null && !data.isEmpty();
    }

    @Override
    public String toString() {
        return "ToolResult{" +
                "success=" + success +
                ", dataSize=" + (data != null ? data.size() : 0) +
                ", errorCode='" + errorCode + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", executeTimeMs=" + executeTimeMs +
                ", dataSource='" + dataSource + '\'' +
                '}';
    }
}
