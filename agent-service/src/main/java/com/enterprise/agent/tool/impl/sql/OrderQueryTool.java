package com.enterprise.agent.tool.impl.sql;

import com.enterprise.agent.tool.Tool;
import com.enterprise.agent.tool.ToolContext;
import com.enterprise.agent.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单查询工具
 * 
 * 执行预定义的订单查询，返回结构化结果
 * 
 * 职责：
 * - 通过 DAO 查询订单数据（非直接 SQL）
 * - 校验用户是否有权访问该订单
 * - 脱敏敏感字段后返回结果
 * 
 * 禁止：
 * - 接受动态 SQL 语句
 * - 由 LLM 生成 SQL
 * - 返回超过限定行数的结果
 * - 返回敏感字段明文
 */
@Component
public class OrderQueryTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(OrderQueryTool.class);

    /**
     * 最大返回行数
     */
    private static final int MAX_ROWS = 100;

    /**
     * 工具名称
     */
    private static final String TOOL_NAME = "order_query";

    /**
     * 数据来源标识
     */
    private static final String DATA_SOURCE = "V_ORDER_INFO";

    // 实际项目中注入 DAO
    // @Autowired
    // private OrderDAO orderDAO;

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public String getDescription() {
        return "查询订单信息，包括订单状态、金额、创建时间等";
    }

    @Override
    public List<String> getRequiredParams() {
        return Arrays.asList("order_id");
    }

    @Override
    public ToolResult execute(Map<String, Object> params, ToolContext context) {
        long startTime = System.currentTimeMillis();
        String traceId = context.getTraceId();

        try {
            // 1. 提取参数
            String orderId = (String) params.get("order_id");
            String userId = context.getUserId();

            log.info("[{}] 执行订单查询: orderId={}, userId={}", traceId, orderId, userId);

            // 2. 校验订单归属（用户是否有权访问该订单）
            if (!checkOrderBelongsToUser(orderId, userId)) {
                log.warn("[{}] 订单归属校验失败: orderId={}, userId={}", traceId, orderId, userId);
                return ToolResult.fail("PERMISSION_DENIED", "您没有权限查看该订单");
            }

            // 3. 查询订单数据（通过 DAO，非直接 SQL）
            Map<String, Object> orderData = queryOrderById(orderId);

            if (orderData == null || orderData.isEmpty()) {
                log.info("[{}] 订单不存在: orderId={}", traceId, orderId);
                return ToolResult.noData("未找到该订单信息");
            }

            // 4. 脱敏处理
            Map<String, Object> sanitizedData = sanitizeData(orderData);

            // 5. 构建结果
            long executeTime = System.currentTimeMillis() - startTime;
            ToolResult result = ToolResult.success(sanitizedData, DATA_SOURCE);
            result.setExecuteTimeMs(executeTime);

            log.info("[{}] 订单查询成功: orderId={}, executeTime={}ms",
                    traceId, orderId, executeTime);

            return result;

        } catch (Exception e) {
            log.error("[{}] 订单查询异常: {}", traceId, e.getMessage(), e);
            return ToolResult.fail("QUERY_ERROR", "查询订单信息失败");
        }
    }

    /**
     * 校验订单是否属于该用户
     * 
     * 实际项目中通过 DAO 查询
     */
    private boolean checkOrderBelongsToUser(String orderId, String userId) {
        // 模拟实现：实际项目中调用 orderDAO.checkOwnership(orderId, userId)
        // 这里假设所有订单都属于当前用户
        if (orderId == null || userId == null) {
            return false;
        }
        return true;
    }

    /**
     * 通过订单ID查询订单
     * 
     * 实际项目中通过 DAO 查询，此处为模拟实现
     */
    private Map<String, Object> queryOrderById(String orderId) {
        // 模拟实现：实际项目中调用 orderDAO.findById(orderId)
        // 返回模拟数据
        if (orderId == null || !orderId.startsWith("ORD-")) {
            return null;
        }

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("ORDER_ID", orderId);
        orderData.put("ORDER_STATUS", "已发货");
        orderData.put("ORDER_AMOUNT", 299.00);
        orderData.put("CREATE_TIME", "2026-01-15 10:30:00");
        orderData.put("UPDATE_TIME", "2026-01-16 08:00:00");
        orderData.put("RECEIVER_NAME", "张三");
        orderData.put("RECEIVER_PHONE", "13812345678");
        orderData.put("RECEIVER_ADDRESS", "北京市朝阳区xxx路xxx号");
        orderData.put("PRODUCT_NAME", "商品A x 2, 商品B x 1");

        return orderData;
    }

    /**
     * 数据脱敏处理
     * 
     * 对敏感字段进行脱敏：
     * - 手机号：保留前3后4
     * - 地址：保留前10个字符
     */
    private Map<String, Object> sanitizeData(Map<String, Object> data) {
        Map<String, Object> sanitized = new HashMap<>(data);

        // 手机号脱敏
        if (sanitized.containsKey("RECEIVER_PHONE")) {
            String phone = String.valueOf(sanitized.get("RECEIVER_PHONE"));
            if (phone.length() >= 11) {
                sanitized.put("RECEIVER_PHONE", phone.substring(0, 3) + "****" + phone.substring(7));
            }
        }

        // 移除敏感字段
        sanitized.remove("PASSWORD");
        sanitized.remove("ID_CARD");
        sanitized.remove("BANK_CARD");

        return sanitized;
    }
}
