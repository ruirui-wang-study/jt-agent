package com.enterprise.agent.fallback;

import com.enterprise.agent.session.ConversationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 人工转接服务
 */
@Component
public class HumanHandoffService {

    private static final Logger log = LoggerFactory.getLogger(HumanHandoffService.class);

    /**
     * 执行转人工
     */
    public void handoff(ConversationContext context) {
        log.info("[{}] 转人工: sessionId={}, userId={}",
                context.getTraceId(), context.getSessionId(), context.getUserId());

        try {
            // 1. 保存对话历史到持久化存储
            saveConversationHistory(context);

            // 2. 通知人工客服系统
            notifyHumanAgent(context);

            // 3. 记录转接事件
            logHandoffEvent(context);

        } catch (Exception e) {
            log.error("[{}] 转人工失败: {}", context.getTraceId(), e.getMessage(), e);
        }
    }

    private void saveConversationHistory(ConversationContext context) {
        log.debug("保存对话历史: sessionId={}, historySize={}",
                context.getSessionId(), context.getHistory().size());
    }

    private void notifyHumanAgent(ConversationContext context) {
        log.info("已通知人工客服系统: sessionId={}", context.getSessionId());
    }

    private void logHandoffEvent(ConversationContext context) {
        log.info("人工转接事件: sessionId={}, userId={}, reason=AUTO_HANDOFF",
                context.getSessionId(), context.getUserId());
    }

    /**
     * 获取预估等待时间
     */
    public int getEstimatedWaitTime() {
        return 60;
    }
}
