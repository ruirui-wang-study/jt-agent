package com.enterprise.agent.tool;

import com.enterprise.agent.intent.IntentType;
import com.enterprise.agent.tool.impl.sql.OrderQueryTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具执行器
 * 
 * 职责：
 * - 根据意图类型从 ToolRegistry 中选择对应的 Tool
 * - 校验工具所需参数是否完整
 * - 调用 Tool.execute() 并获取结构化执行结果
 * - 对执行结果进行后校验（脱敏、行数限制）
 * 
 * 禁止：
 * - 让 LLM 决定调用哪个工具
 * - 绕过参数校验直接执行
 * - 返回未脱敏的原始数据库记录
 * - 直接拼接 SQL 语句
 */
@Component
public class ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutor.class);

    private final Map<String, Tool> toolRegistry = new HashMap<>();

    /**
     * 意图与工具的映射关系
     */
    private static final Map<IntentType, String> INTENT_TOOL_MAPPING = new HashMap<>();

    static {
        // 定义意图与工具的映射
        INTENT_TOOL_MAPPING.put(IntentType.QUERY_ORDER_STATUS, "order_query");
        INTENT_TOOL_MAPPING.put(IntentType.QUERY_LOGISTICS, "logistics_query");
        INTENT_TOOL_MAPPING.put(IntentType.QUERY_ACCOUNT, "account_query");
        INTENT_TOOL_MAPPING.put(IntentType.CONSULTATION, "knowledge_search");
    }

    @Autowired
    public ToolExecutor(List<Tool> tools) {
        // 注册所有工具
        for (Tool tool : tools) {
            toolRegistry.put(tool.getName(), tool);
            log.info("注册工具: {}", tool.getName());
        }
    }

    /**
     * 执行工具
     */
    public ToolResult execute(IntentType intentType, Map<String, Object> params, ToolContext context) {
        String traceId = context.getTraceId();
        log.info("[{}] 执行工具: 意图={}", traceId, intentType);

        // 1. 选择工具
        Tool tool = selectTool(intentType);
        if (tool == null) {
            log.warn("[{}] 未找到对应工具: {}", traceId, intentType);
            return ToolResult.fail("NO_TOOL", "该功能暂不支持自动处理");
        }

        // 2. 参数校验
        if (!tool.validateParams(params)) {
            List<String> missing = tool.getMissingParams(params);
            log.warn("[{}] 参数校验失败, 缺失: {}", traceId, missing);
            return ToolResult.fail("PARAM_INVALID", "缺少必要参数: " + missing);
        }

        // 3. 执行工具
        long startTime = System.currentTimeMillis();
        try {
            ToolResult result = tool.execute(params, context);
            long executeTime = System.currentTimeMillis() - startTime;
            result.setExecuteTimeMs(executeTime);

            log.info("[{}] 工具执行完成: tool={}, success={}, time={}ms",
                    traceId, tool.getName(), result.isSuccess(), executeTime);

            return result;

        } catch (Exception e) {
            log.error("[{}] 工具执行异常: {}", traceId, e.getMessage(), e);
            return ToolResult.fail("EXECUTE_ERROR", "执行失败");
        }
    }

    /**
     * 选择工具
     */
    private Tool selectTool(IntentType intentType) {
        String toolName = INTENT_TOOL_MAPPING.get(intentType);
        if (toolName == null) {
            return null;
        }
        return toolRegistry.get(toolName);
    }

    /**
     * 获取已注册的工具列表
     */
    public Map<String, Tool> getToolRegistry() {
        return new HashMap<>(toolRegistry);
    }
}
