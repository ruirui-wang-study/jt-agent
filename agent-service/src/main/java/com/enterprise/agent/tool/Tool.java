package com.enterprise.agent.tool;

import java.util.List;
import java.util.Map;

/**
 * 工具接口
 * 
 * 所有业务能力必须实现此接口才能被 ToolExecutor 调用
 * 
 * 职责：
 * - 定义工具的标准契约
 * - 接收结构化参数，返回结构化结果
 * 
 * 禁止：
 * - 在接口层定义任何业务逻辑
 * - 定义与执行无关的方法
 */
public interface Tool {

    /**
     * 获取工具名称（唯一标识）
     * 
     * @return 工具名称
     */
    String getName();

    /**
     * 获取工具描述
     * 
     * @return 工具描述
     */
    String getDescription();

    /**
     * 获取该工具需要的必填参数列表
     * 
     * @return 必填参数名列表
     */
    List<String> getRequiredParams();

    /**
     * 执行工具
     * 
     * @param params  执行参数（来自槽位抽取的结构化数据）
     * @param context 执行上下文（包含用户信息、权限等）
     * @return 执行结果
     */
    ToolResult execute(Map<String, Object> params, ToolContext context);

    /**
     * 校验参数是否满足执行条件
     * 
     * @param params 待校验参数
     * @return 是否通过校验
     */
    default boolean validateParams(Map<String, Object> params) {
        if (params == null) {
            return getRequiredParams().isEmpty();
        }
        for (String required : getRequiredParams()) {
            Object value = params.get(required);
            if (value == null || (value instanceof String && ((String) value).isEmpty())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取参数校验失败的详细信息
     * 
     * @param params 待校验参数
     * @return 缺失的参数列表
     */
    default List<String> getMissingParams(Map<String, Object> params) {
        java.util.List<String> missing = new java.util.ArrayList<>();
        if (params == null) {
            missing.addAll(getRequiredParams());
            return missing;
        }
        for (String required : getRequiredParams()) {
            Object value = params.get(required);
            if (value == null || (value instanceof String && ((String) value).isEmpty())) {
                missing.add(required);
            }
        }
        return missing;
    }
}
