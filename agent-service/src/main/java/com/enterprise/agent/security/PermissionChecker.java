package com.enterprise.agent.security;

import com.enterprise.agent.intent.IntentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 权限校验器
 */
@Component
public class PermissionChecker {

    private static final Logger log = LoggerFactory.getLogger(PermissionChecker.class);

    /**
     * 校验权限
     */
    public boolean checkPermission(String userId, IntentResult intentResult) {
        try {
            Map<String, Object> slots = intentResult.getSlots();
            if (slots == null) {
                return true;
            }

            // 检查订单归属权限
            String orderId = (String) slots.get("order_id");
            if (orderId != null) {
                return checkOrderBelongsToUser(userId, orderId);
            }

            return true;

        } catch (Exception e) {
            log.error("权限校验异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 检查订单是否属于用户
     */
    private boolean checkOrderBelongsToUser(String userId, String orderId) {
        // 实际项目中通过 DAO 查询
        // 这里模拟：所有订单都属于当前用户
        if (orderId == null || userId == null) {
            return false;
        }
        return true;
    }
}
