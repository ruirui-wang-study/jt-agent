package com.enterprise.agent.orchestrator;

import com.enterprise.agent.controller.dto.ChatResponse;
import com.enterprise.agent.fallback.FallbackManager;
import com.enterprise.agent.fallback.HumanHandoffService;
import com.enterprise.agent.intent.IntentRecognizer;
import com.enterprise.agent.intent.IntentResult;
import com.enterprise.agent.intent.IntentType;
import com.enterprise.agent.intent.SlotExtractor;
import com.enterprise.agent.response.ResponseGenerator;
import com.enterprise.agent.security.PermissionChecker;
import com.enterprise.agent.security.SensitiveWordFilter;
import com.enterprise.agent.session.ConversationContext;
import com.enterprise.agent.tool.ToolContext;
import com.enterprise.agent.tool.ToolExecutor;
import com.enterprise.agent.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Agent 核心编排器
 * 
 * 职责：
 * - 作为整个 Agent 的调度中心，控制请求处理的全流程
 * - 维护代码级状态机，根据各模块返回结果决定状态流转
 * - 按固定顺序调用：意图识别 → 槽位抽取 → 权限校验 → 工具执行 → 响应生成
 * - 处理各类异常场景，触发对应的兜底策略
 * - 确保每次请求最终到达终态（DONE / HANDOFF / REJECT / ERROR）
 * 
 * 禁止：
 * - 直接调用 LLM（必须通过 IntentRecognizer / ResponseGenerator）
 * - 直接访问数据库或外部 API（必须通过 ToolExecutor）
 * - 生成任何自由文本内容
 * - 将"下一步做什么"的决策权交给 LLM
 */
@Component
public class AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);

    private final StateMachine stateMachine;
    private final IntentRecognizer intentRecognizer;
    private final SlotExtractor slotExtractor;
    private final ToolExecutor toolExecutor;
    private final ResponseGenerator responseGenerator;
    private final PermissionChecker permissionChecker;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final FallbackManager fallbackManager;
    private final HumanHandoffService humanHandoffService;

    @Autowired
    public AgentOrchestrator(StateMachine stateMachine,
            IntentRecognizer intentRecognizer,
            SlotExtractor slotExtractor,
            ToolExecutor toolExecutor,
            ResponseGenerator responseGenerator,
            PermissionChecker permissionChecker,
            SensitiveWordFilter sensitiveWordFilter,
            FallbackManager fallbackManager,
            HumanHandoffService humanHandoffService) {
        this.stateMachine = stateMachine;
        this.intentRecognizer = intentRecognizer;
        this.slotExtractor = slotExtractor;
        this.toolExecutor = toolExecutor;
        this.responseGenerator = responseGenerator;
        this.permissionChecker = permissionChecker;
        this.sensitiveWordFilter = sensitiveWordFilter;
        this.fallbackManager = fallbackManager;
        this.humanHandoffService = humanHandoffService;
    }

    /**
     * 处理用户请求
     *
     * @param context     对话上下文
     * @param userMessage 用户输入
     * @return ChatResponse
     */
    public ChatResponse process(ConversationContext context, String userMessage) {
        String traceId = context.getTraceId();
        log.info("[{}] 开始处理请求: sessionId={}", traceId, context.getSessionId());

        // 1. 敏感词检查
        if (sensitiveWordFilter.containsSensitiveWord(userMessage)) {
            log.warn("[{}] 检测到敏感词", traceId);
            stateMachine.transition(context, AgentState.REJECT);
            return ChatResponse.error(400, "您的输入包含敏感内容，无法处理。");
        }

        // 2. 添加用户消息到历史
        context.addUserMessage(userMessage);

        // 3. 状态机初始化
        stateMachine.transition(context, AgentState.INTENT_RECOGNITION);

        try {
            // 4. 意图识别
            IntentResult intentResult = intentRecognizer.recognize(context, userMessage);
            log.info("[{}] 意图识别结果: intent={}, confidence={}",
                    traceId, intentResult.getIntentType(), intentResult.getConfidence());

            // 5. 处理特殊意图
            ChatResponse specialResponse = handleSpecialIntent(context, intentResult);
            if (specialResponse != null) {
                return specialResponse;
            }

            // 6. 未识别意图处理
            if (!intentResult.isRecognized()) {
                return handleUnknownIntent(context, intentResult);
            }

            // 7. 槽位抽取
            stateMachine.transition(context, AgentState.SLOT_EXTRACTION);
            intentResult = slotExtractor.extractSlots(intentResult);

            // 8. 检查槽位完整性
            if (!intentResult.isSlotsComplete()) {
                return handleMissingSlots(context, intentResult);
            }

            // 9. 权限校验
            stateMachine.transition(context, AgentState.SLOT_COMPLETE);
            if (!permissionChecker.checkPermission(context.getUserId(), intentResult)) {
                stateMachine.transition(context, AgentState.FORBIDDEN);
                stateMachine.transition(context, AgentState.REJECT);
                return ChatResponse.error(403, "您没有权限执行此操作。");
            }

            // 10. 工具执行
            stateMachine.transition(context, AgentState.TOOL_EXECUTION);
            ToolContext toolContext = buildToolContext(context);
            ToolResult toolResult = toolExecutor.execute(
                    intentResult.getIntentType(),
                    intentResult.getSlots(),
                    toolContext);

            // 11. 工具执行失败处理
            if (!toolResult.isSuccess()) {
                return handleToolFailure(context, toolResult);
            }

            // 12. 生成响应
            stateMachine.transition(context, AgentState.RESPONSE_GENERATION);
            String reply = responseGenerator.generate(intentResult, toolResult);

            // 13. 响应校验与过滤
            reply = sensitiveWordFilter.filter(reply);

            // 14. 完成
            stateMachine.transition(context, AgentState.DONE);
            context.addAssistantMessage(reply);
            context.resetUnknownIntentCount();

            log.info("[{}] 处理完成", traceId);
            return ChatResponse.success(reply, context.getSessionId());

        } catch (Exception e) {
            log.error("[{}] 处理请求异常: {}", traceId, e.getMessage(), e);
            stateMachine.transition(context, AgentState.ERROR);
            return fallbackManager.handleError(context, e);
        }
    }

    /**
     * 处理特殊意图（转人工、闲聊、结束对话）
     */
    private ChatResponse handleSpecialIntent(ConversationContext context, IntentResult intentResult) {
        IntentType intentType = intentResult.getIntentType();
        String traceId = context.getTraceId();

        // 用户请求转人工
        if (intentType == IntentType.HUMAN_HANDOFF) {
            log.info("[{}] 用户请求转人工", traceId);
            stateMachine.transition(context, AgentState.HUMAN_HANDOFF);
            humanHandoffService.handoff(context);
            stateMachine.transition(context, AgentState.DONE);
            return ChatResponse.handoff("正在为您转接人工客服，请稍候...", context.getSessionId());
        }

        // 闲聊/打招呼
        if (intentType == IntentType.GREETING) {
            log.info("[{}] 闲聊问候", traceId);
            stateMachine.transition(context, AgentState.DONE);
            String reply = "您好！我是智能客服助手，请问有什么可以帮您？";
            context.addAssistantMessage(reply);
            return ChatResponse.success(reply, context.getSessionId());
        }

        // 结束对话
        if (intentType == IntentType.END_CONVERSATION) {
            log.info("[{}] 结束对话", traceId);
            stateMachine.transition(context, AgentState.DONE);
            String reply = "感谢您的使用，再见！如有需要请随时联系。";
            context.addAssistantMessage(reply);
            return ChatResponse.success(reply, context.getSessionId());
        }

        return null; // 非特殊意图
    }

    /**
     * 处理未识别意图
     */
    private ChatResponse handleUnknownIntent(ConversationContext context, IntentResult intentResult) {
        String traceId = context.getTraceId();
        stateMachine.transition(context, AgentState.UNKNOWN_INTENT);

        // 增加失败计数
        int failCount = context.incrementUnknownIntentCount();
        log.info("[{}] 意图未识别, failCount={}", traceId, failCount);

        // 超过3次转人工
        if (failCount >= 3) {
            log.info("[{}] 连续3次未识别, 转人工", traceId);
            stateMachine.transition(context, AgentState.HUMAN_HANDOFF);
            humanHandoffService.handoff(context);
            stateMachine.transition(context, AgentState.DONE);
            String reply = "抱歉，我暂时无法理解您的问题，正在为您转接人工客服。";
            context.addAssistantMessage(reply);
            return ChatResponse.handoff(reply, context.getSessionId());
        }

        // 引导追问
        stateMachine.transition(context, AgentState.ASK_USER);
        stateMachine.transition(context, AgentState.DONE);

        String reply = "抱歉，我没有完全理解您的意思。您是想：\n" +
                "1. 查询订单状态\n" +
                "2. 查询物流信息\n" +
                "3. 咨询其他问题\n" +
                "请选择或重新描述您的问题。";
        context.addAssistantMessage(reply);

        return ChatResponse.needClarify(reply, context.getSessionId());
    }

    /**
     * 处理槽位缺失
     */
    private ChatResponse handleMissingSlots(ConversationContext context, IntentResult intentResult) {
        String traceId = context.getTraceId();
        log.info("[{}] 槽位缺失: {}", traceId, intentResult.getMissingSlots());

        stateMachine.transition(context, AgentState.NEED_CLARIFY);

        // 保存当前意图，等待用户补充
        context.setPendingIntent(intentResult);

        // 生成询问消息
        String askMessage = generateMissingSlotQuestion(intentResult);

        stateMachine.transition(context, AgentState.ASK_USER);
        stateMachine.transition(context, AgentState.DONE);

        context.addAssistantMessage(askMessage);
        return ChatResponse.needClarify(askMessage, context.getSessionId());
    }

    /**
     * 处理工具执行失败
     */
    private ChatResponse handleToolFailure(ConversationContext context, ToolResult toolResult) {
        String traceId = context.getTraceId();
        log.warn("[{}] 工具执行失败: {}", traceId, toolResult.getErrorMessage());

        stateMachine.transition(context, AgentState.FALLBACK);

        // 无数据情况
        if (!toolResult.hasData() && toolResult.getErrorMessage() != null) {
            stateMachine.transition(context, AgentState.DONE);
            String reply = toolResult.getErrorMessage();
            context.addAssistantMessage(reply);
            return ChatResponse.success(reply, context.getSessionId());
        }

        // 系统错误转人工
        log.info("[{}] 工具执行失败, 转人工", traceId);
        stateMachine.transition(context, AgentState.HUMAN_HANDOFF);
        humanHandoffService.handoff(context);
        stateMachine.transition(context, AgentState.DONE);

        String reply = "抱歉，系统暂时无法处理您的请求，正在为您转接人工客服。";
        context.addAssistantMessage(reply);
        return ChatResponse.handoff(reply, context.getSessionId());
    }

    /**
     * 生成缺失槽位询问消息
     */
    private String generateMissingSlotQuestion(IntentResult intentResult) {
        StringBuilder sb = new StringBuilder("为了帮您处理，请提供以下信息：\n");
        for (String slot : intentResult.getMissingSlots()) {
            switch (slot) {
                case "order_id":
                    sb.append("- 订单编号（格式：ORD-XXXX-XXXXXX）\n");
                    break;
                case "refund_reason":
                    sb.append("- 退款原因\n");
                    break;
                case "complaint_content":
                    sb.append("- 投诉内容\n");
                    break;
                default:
                    sb.append("- ").append(slot).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 构建工具执行上下文
     */
    private ToolContext buildToolContext(ConversationContext context) {
        return ToolContext.builder()
                .userId(context.getUserId())
                .sessionId(context.getSessionId())
                .traceId(context.getTraceId())
                .permissionLevel(context.getPermissionLevel())
                .debug(false)
                .build();
    }
}
