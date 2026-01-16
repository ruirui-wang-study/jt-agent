package com.enterprise.agent.controller;

import com.enterprise.agent.controller.dto.ChatRequest;
import com.enterprise.agent.controller.dto.ChatResponse;
import com.enterprise.agent.log.AgentLogger;
import com.enterprise.agent.orchestrator.AgentOrchestrator;
import com.enterprise.agent.security.RiskController;
import com.enterprise.agent.session.ConversationContext;
import com.enterprise.agent.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Agent 主入口 Controller
 * 
 * 职责：
 * - 接收 HTTP 请求，校验请求格式
 * - 生成 traceId，贯穿全链路
 * - 调用风控层进行前置拦截
 * - 将请求转发给 Orchestrator
 * - 返回响应
 * 
 * 禁止：
 * - 在此层做任何业务逻辑判断
 * - 在此层调用 LLM
 * - 在此层直接访问数据库
 * - 在此层处理意图识别或槽位抽取
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final AgentOrchestrator orchestrator;
    private final SessionManager sessionManager;
    private final RiskController riskController;
    private final AgentLogger agentLogger;

    @Autowired
    public AgentController(AgentOrchestrator orchestrator,
            SessionManager sessionManager,
            RiskController riskController,
            AgentLogger agentLogger) {
        this.orchestrator = orchestrator;
        this.sessionManager = sessionManager;
        this.riskController = riskController;
        this.agentLogger = agentLogger;
    }

    /**
     * 对话接口
     */
    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        // 1. 生成追踪ID
        String traceId = generateTraceId();

        try {
            // 2. 参数校验
            String validateError = request.validate();
            if (validateError != null) {
                log.warn("[{}] 请求参数校验失败: {}", traceId, validateError);
                ChatResponse response = ChatResponse.error(400, validateError);
                response.setTraceId(traceId);
                return response;
            }

            // 3. 记录请求日志
            agentLogger.logRequest(traceId, request.getSessionId(),
                    request.getUserId(), request.getMessage());

            // 4. 风控检查（限流、黑名单等）
            if (!riskController.checkRisk(request.getUserId(), request.getMessage())) {
                log.warn("[{}] 风控拦截: userId={}", traceId, request.getUserId());
                ChatResponse response = ChatResponse.error(403, "请求被拒绝，请稍后重试");
                response.setTraceId(traceId);
                agentLogger.logResponse(traceId, response);
                return response;
            }

            // 5. 加载或创建会话上下文
            ConversationContext context = sessionManager.getOrCreate(
                    request.getSessionId(),
                    request.getUserId());
            context.setTraceId(traceId);

            // 6. 调用编排器处理
            ChatResponse response = orchestrator.process(context, request.getMessage());
            response.setTraceId(traceId);
            response.setSessionId(request.getSessionId());

            // 7. 保存会话状态
            sessionManager.save(context);

            // 8. 记录响应日志
            agentLogger.logResponse(traceId, response);

            return response;

        } catch (Exception e) {
            log.error("[{}] Agent 处理异常: {}", traceId, e.getMessage(), e);
            agentLogger.logError(traceId, e);

            ChatResponse response = ChatResponse.error(500, "系统繁忙，请稍后重试");
            response.setTraceId(traceId);
            return response;
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    /**
     * 结束会话
     */
    @PostMapping("/session/end")
    public ChatResponse endSession(@RequestParam String sessionId) {
        String traceId = generateTraceId();
        log.info("[{}] 结束会话: sessionId={}", traceId, sessionId);

        sessionManager.remove(sessionId);

        ChatResponse response = ChatResponse.success("会话已结束", sessionId);
        response.setTraceId(traceId);
        return response;
    }

    /**
     * 生成追踪ID
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
