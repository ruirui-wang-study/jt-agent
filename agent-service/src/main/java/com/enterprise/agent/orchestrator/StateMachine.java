package com.enterprise.agent.orchestrator;

import com.enterprise.agent.session.ConversationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 状态机实现
 * 
 * 代码级状态机，非 Prompt 控制
 * 明确定义所有状态转换规则
 */
@Component
public class StateMachine {

        private static final Logger log = LoggerFactory.getLogger(StateMachine.class);

        /**
         * 状态转换规则
         */
        private static final Map<AgentState, Set<AgentState>> TRANSITIONS = new HashMap<>();

        static {
                // 定义合法的状态转换
                TRANSITIONS.put(AgentState.INIT, setOf(
                                AgentState.INTENT_RECOGNITION));

                TRANSITIONS.put(AgentState.INTENT_RECOGNITION, setOf(
                                AgentState.SLOT_EXTRACTION,
                                AgentState.UNKNOWN_INTENT,
                                AgentState.HUMAN_HANDOFF,
                                AgentState.DONE,
                                AgentState.ERROR));

                TRANSITIONS.put(AgentState.SLOT_EXTRACTION, setOf(
                                AgentState.SLOT_COMPLETE,
                                AgentState.NEED_CLARIFY,
                                AgentState.FORBIDDEN,
                                AgentState.ERROR));

                TRANSITIONS.put(AgentState.SLOT_COMPLETE, setOf(
                                AgentState.TOOL_EXECUTION,
                                AgentState.FORBIDDEN));

                TRANSITIONS.put(AgentState.NEED_CLARIFY, setOf(
                                AgentState.ASK_USER,
                                AgentState.FALLBACK,
                                AgentState.HUMAN_HANDOFF));

                TRANSITIONS.put(AgentState.UNKNOWN_INTENT, setOf(
                                AgentState.ASK_USER,
                                AgentState.FALLBACK,
                                AgentState.HUMAN_HANDOFF));

                TRANSITIONS.put(AgentState.FORBIDDEN, setOf(
                                AgentState.REJECT,
                                AgentState.HUMAN_HANDOFF));

                TRANSITIONS.put(AgentState.TOOL_EXECUTION, setOf(
                                AgentState.RESPONSE_GENERATION,
                                AgentState.FALLBACK,
                                AgentState.ERROR));

                TRANSITIONS.put(AgentState.RESPONSE_GENERATION, setOf(
                                AgentState.DONE,
                                AgentState.ERROR));

                TRANSITIONS.put(AgentState.ASK_USER, setOf(
                                AgentState.DONE));

                TRANSITIONS.put(AgentState.FALLBACK, setOf(
                                AgentState.HUMAN_HANDOFF,
                                AgentState.DONE));

                TRANSITIONS.put(AgentState.HUMAN_HANDOFF, setOf(
                                AgentState.DONE));

                TRANSITIONS.put(AgentState.REJECT, setOf(
                                AgentState.DONE));

                TRANSITIONS.put(AgentState.ERROR, setOf(
                                AgentState.DONE,
                                AgentState.FALLBACK));
        }

        /**
         * Java 8 兼容的 Set 创建方法
         */
        @SafeVarargs
        private static <T> Set<T> setOf(T... elements) {
                return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(elements)));
        }

        /**
         * 执行状态转换
         */
        public boolean transition(ConversationContext context, AgentState targetState) {
                AgentState currentState = context.getState();

                // 检查转换是否合法
                if (!isValidTransition(currentState, targetState)) {
                        log.warn("[{}] 非法状态转换: {} -> {}",
                                        context.getTraceId(), currentState, targetState);
                        return false;
                }

                // 记录状态转换历史
                context.addStateHistory(currentState, targetState);

                // 更新状态
                context.setState(targetState);

                log.debug("[{}] 状态转换: {} -> {}",
                                context.getTraceId(), currentState, targetState);
                return true;
        }

        /**
         * 检查状态转换是否合法
         */
        public boolean isValidTransition(AgentState from, AgentState to) {
                if (from == null || to == null) {
                        return false;
                }
                Set<AgentState> validTargets = TRANSITIONS.get(from);
                return validTargets != null && validTargets.contains(to);
        }

        /**
         * 获取当前状态可转换的目标状态
         */
        public Set<AgentState> getValidTransitions(AgentState currentState) {
                Set<AgentState> transitions = TRANSITIONS.get(currentState);
                return transitions != null ? transitions : Collections.emptySet();
        }

        /**
         * 重置状态机
         */
        public void reset(ConversationContext context) {
                context.setState(AgentState.INIT);
                context.clearStateHistory();
        }
}
