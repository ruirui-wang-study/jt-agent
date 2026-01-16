package com.enterprise.agent.fallback;

import com.enterprise.agent.controller.dto.ChatResponse;
import com.enterprise.agent.session.ConversationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 兜底管理器
 */
@Component
public class FallbackManager {

    private static final Logger log = LoggerFactory.getLogger(FallbackManager.class);

    private final HumanHandoffService humanHandoffService;

    @Autowired
    public FallbackManager(HumanHandoffService humanHandoffService) {
        this.humanHandoffService = humanHandoffService;
    }

    /**
     * 处理错误
     */
    public ChatResponse handleError(ConversationContext context, Exception e) {
        log.error("[{}] 处理请求时发生错误: {}",
                context.getTraceId(), e.getMessage());

        String errorType = e.getClass().getSimpleName();

        switch (errorType) {
            case "TimeoutException":
                return ChatResponse.error(504, "系统响应超时，请稍后重试。");

            case "SQLException":
            case "DataAccessException":
                humanHandoffService.handoff(context);
                return ChatResponse.handoff(
                        "系统暂时无法处理您的请求，正在为您转接人工客服。",
                        context.getSessionId());

            default:
                return ChatResponse.error(500, "抱歉，系统遇到了一些问题。请稍后重试，或拨打客服热线。");
        }
    }

    /**
     * 获取兜底回复
     */
    public String getFallbackReply() {
        return "抱歉，我暂时无法回答您的问题。您可以：\n" +
                "1. 尝试换一种方式描述问题\n" +
                "2. 输入\"转人工\"联系人工客服\n" +
                "3. 拨打客服热线 400-xxx-xxxx";
    }
}
