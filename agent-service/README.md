# 企业级智能客服 Agent 系统

## 第一部分：项目总体说明

### 1.1 项目定位

本系统是一个**后端主导的大模型协作系统**，非传统聊天机器人。核心定位：

- **业务驱动**：所有业务逻辑由后端代码控制，LLM 仅作为"语言理解"和"语言生成"工具
- **可控可审计**：每一步决策均有日志，可追溯、可复现
- **高准确性**：通过结构化流程确保回答基于真实数据，杜绝凭空编造

### 1.2 核心设计思想

```
+------------------+     +------------------+     +------------------+
|   用户输入       | --> |   意图识别(LLM)   | --> |  参数结构化抽取  |
+------------------+     +------------------+     +------------------+
                                                           |
                                                           v
+------------------+     +------------------+     +------------------+
|   自然语言回复   | <-- |  LLM表达格式化    | <-- |  工具执行(SQL等) |
+------------------+     +------------------+     +------------------+
```

**关键原则**：
1. **LLM 不做决策**：LLM 只负责"理解"和"表达"，不参与业务判断
2. **数据来源单一**：所有业务数据必须来自预定义的 Tool（SQL/RAG/API）
3. **参数显式校验**：缺失参数不推断，而是明确询问用户
4. **状态机控制流程**：对话状态由代码级 FSM 管理，非 Prompt 控制

### 1.3 为什么该设计能避免幻觉

| 幻觉来源 | 本设计如何规避 |
|---------|--------------|
| LLM 编造数据 | LLM 无权访问数据库，只能表达 Tool 返回的结构化结果 |
| LLM 推断缺失信息 | 参数校验层强制要求完整，缺失则进入"澄清"状态 |
| LLM 自由发挥 | 响应模板固定，LLM 只填充数据占位符 |
| 上下文混淆 | 会话管理层维护结构化历史，非自由文本拼接 |

---

## 第二部分：项目目录结构

```
agent-service/
├── pom.xml                                    # Maven 配置
├── src/main/java/com/enterprise/agent/
│   ├── AgentApplication.java                  # Spring Boot 启动类
│   │
│   ├── controller/                            # API 接入层
│   │   ├── AgentController.java               # Agent 主入口
│   │   └── dto/
│   │       ├── AgentRequest.java              # 请求 DTO
│   │       └── AgentResponse.java             # 响应 DTO
│   │
│   ├── session/                               # 会话与上下文管理层
│   │   ├── SessionManager.java                # 会话管理器
│   │   ├── ConversationContext.java           # 对话上下文
│   │   └── SessionStore.java                  # 会话存储接口
│   │
│   ├── orchestrator/                          # Agent 编排层
│   │   ├── AgentOrchestrator.java             # 核心编排器
│   │   ├── AgentState.java                    # 状态枚举
│   │   └── StateMachine.java                  # 状态机实现
│   │
│   ├── intent/                                # 意图识别层
│   │   ├── IntentRecognizer.java              # 意图识别器
│   │   ├── IntentResult.java                  # 意图结果结构
│   │   ├── IntentType.java                    # 意图类型枚举
│   │   └── SlotExtractor.java                 # 槽位抽取器
│   │
│   ├── tool/                                  # 工具层
│   │   ├── Tool.java                          # 工具接口
│   │   ├── ToolExecutor.java                  # 工具执行器
│   │   ├── ToolResult.java                    # 工具执行结果
│   │   ├── impl/
│   │   │   ├── SqlQueryTool.java              # SQL 查询工具
│   │   │   ├── RagSearchTool.java             # RAG 检索工具
│   │   │   └── ApiCallTool.java               # API 调用工具
│   │   └── validation/
│   │       └── ParameterValidator.java        # 参数校验器
│   │
│   ├── security/                              # 权限校验 & 风控层
│   │   ├── PermissionChecker.java             # 权限校验器
│   │   ├── RiskController.java                # 风控控制器
│   │   └── SensitiveWordFilter.java           # 敏感词过滤
│   │
│   ├── llm/                                   # LLM 接入层
│   │   ├── LLMClient.java                     # LLM 客户端接口
│   │   ├── LLMRequest.java                    # LLM 请求结构
│   │   ├── LLMResponse.java                   # LLM 响应结构
│   │   └── impl/
│   │       └── DeepSeekLLMClient.java         # DeepSeek 实现
│   │
│   ├── fallback/                              # 兜底与人工接管
│   │   ├── FallbackManager.java               # 兜底管理器
│   │   └── HumanHandoffService.java           # 人工转接服务
│   │
│   ├── log/                                   # 日志 & 评估层
│   │   ├── AgentLogger.java                   # Agent 专用日志
│   │   ├── AuditTrail.java                    # 审计追踪
│   │   └── MetricsCollector.java              # 指标收集
│   │
│   └── config/                                # 配置
│       ├── AgentConfig.java                   # Agent 配置
│       ├── LLMConfig.java                     # LLM 配置
│       └── ToolConfig.java                    # 工具配置
│
├── src/main/resources/
│   ├── application.yml                        # Spring 配置
│   ├── prompts/                               # Prompt 模板
│   │   ├── intent_recognition.txt             # 意图识别 Prompt
│   │   ├── slot_extraction.txt                # 槽位抽取 Prompt
│   │   └── response_generation.txt            # 响应生成 Prompt
│   └── tools/                                 # 工具配置
│       └── tool_definitions.json              # 工具定义
│
└── src/test/java/                             # 单元测试
```

---

## 第三部分：核心模块设计

### 3.1 AgentOrchestrator（核心编排器）

**职责**：
- 管理整个对话流程的状态流转
- 协调各模块（意图识别、工具执行、LLM表达）的调用顺序
- 实现代码级状态机，确保流程可控

**状态流转**：
```
INIT --> INTENT_RECOGNITION --> SLOT_EXTRACTION
                                      |
                    +--------+--------+--------+
                    |        |        |        |
                    v        v        v        v
             SLOT_COMPLETE  NEED_CLARIFY  UNKNOWN_INTENT  FORBIDDEN
                    |            |              |             |
                    v            v              v             v
             TOOL_EXECUTION  ASK_USER     FALLBACK      REJECT
                    |            |              |
                    v            |              v
             RESPONSE_GEN  <----+         HUMAN_HANDOFF
                    |
                    v
                  DONE
```

### 3.2 IntentRecognizer（意图识别器）

**职责**：
- 调用 LLM 识别用户意图
- 将自然语言映射到预定义的 IntentType 枚举
- 返回置信度分数，低于阈值则进入兜底

**关键约束**：
- 意图类型必须在预定义枚举内
- 不识别的意图标记为 UNKNOWN，不允许 LLM 自创类型

### 3.3 SlotExtractor（槽位抽取器）

**职责**：
- 根据意图类型，从用户输入中抽取必要参数
- 返回结构化的 Map<String, Object>
- 标记哪些参数缺失

**关键约束**：
- 只抽取明确提及的信息
- 缺失参数不推断，返回 null
- 参数类型强校验（日期格式、数字范围等）

### 3.4 ToolExecutor（工具执行器）

**职责**：
- 根据意图选择对应的 Tool
- 执行前进行权限校验
- 执行后验证结果合法性

**支持的工具类型**：
| 工具类型 | 实现类 | 数据来源 |
|---------|--------|---------|
| SQL查询 | SqlQueryTool | Oracle 数据库视图 |
| RAG检索 | RagSearchTool | 向量数据库 |
| API调用 | ApiCallTool | 外部服务 |

### 3.5 SqlQueryTool（SQL 查询工具）

**职责**：
- 执行预定义的 SQL 模板（非 LLM 生成）
- 通过 DAO / View 访问 Oracle
- 返回结构化查询结果

**关键约束**：
- SQL 模板预先定义，LLM 只提供参数值
- 参数使用 PreparedStatement 防注入
- 查询结果有行数上限（防止数据泄露）

### 3.6 RagSearchTool（RAG 检索工具）

**职责**：
- 将查询转换为向量
- 在知识库中检索相似内容
- 返回 Top-K 结果及相似度分数

**关键约束**：
- 相似度低于阈值的结果丢弃
- 返回结果带来源标注（可追溯）

### 3.7 LLMClient（LLM 客户端）

**职责**：
- 抽象 LLM 调用接口
- 管理 Prompt 模板
- 处理响应解析

**关键约束**：
- 所有 LLM 调用必须使用模板
- 不允许直接拼接用户原始输入到 Prompt
- 超时和重试机制

### 3.8 FallbackManager（兜底管理器）

**职责**：
- 管理各类异常情况的兜底策略
- 决定何时转人工、何时拒答

**兜底策略**：
| 触发条件 | 处理方式 |
|---------|---------|
| 意图识别失败（UNKNOWN） | 引导式追问 + 3次失败转人工 |
| 参数抽取不完整 | 明确询问缺失参数 |
| 工具执行失败 | 系统错误提示 + 转人工 |
| 敏感词检测 | 拒绝回答 + 记录审计 |
| 无权限访问 | 拒绝 + 说明原因 |

### 3.9 HumanHandoffService（人工转接服务）

**职责**：
- 封装转人工逻辑
- 保存对话上下文供人工客服查看
- 通知在线客服系统

---

## 第四部分：关键接口与类定义

以下为完整可编译的 Java 代码，请查看项目目录中各文件。

---

## 第五部分：完整请求执行链路

### 示例场景：用户查询订单状态

**用户输入**："我想查一下订单 ORD-2026-001234 的物流状态"

#### 步骤 1：请求接收（Controller）

```
输入: HTTP POST /api/agent/chat
      {
        "sessionId": "sess_abc123",
        "userId": "user_001",
        "message": "我想查一下订单 ORD-2026-001234 的物流状态"
      }

输出: 转发到 AgentOrchestrator

失败兜底: 参数校验失败返回 400
```

#### 步骤 2：会话加载（SessionManager）

```
输入: sessionId = "sess_abc123"

输出: ConversationContext（含历史对话、用户信息）

失败兜底: 会话不存在则创建新会话
```

#### 步骤 3：意图识别（IntentRecognizer）

```
输入: 用户消息 + 对话历史

LLM Prompt:
"""
请识别以下用户输入的意图，只能从这些选项中选择：
- QUERY_ORDER_STATUS: 查询订单状态
- QUERY_LOGISTICS: 查询物流信息
- COMPLAINT: 投诉
- UNKNOWN: 无法识别

用户输入: 我想查一下订单 ORD-2026-001234 的物流状态

请以 JSON 格式返回: {"intent": "xxx", "confidence": 0.xx}
"""

输出: IntentResult{type=QUERY_LOGISTICS, confidence=0.95}

失败兜底: confidence < 0.7 或 type=UNKNOWN → 进入澄清流程
```

#### 步骤 4：槽位抽取（SlotExtractor）

```
输入: 意图类型 + 用户消息

LLM Prompt:
"""
根据意图 QUERY_LOGISTICS，从用户输入中提取以下参数：
- order_id: 订单编号（格式：ORD-YYYY-XXXXXX）

用户输入: 我想查一下订单 ORD-2026-001234 的物流状态

请以 JSON 格式返回: {"order_id": "xxx"}
如果无法提取，返回 null
"""

输出: Map{"order_id": "ORD-2026-001234"}

失败兜底: 必填参数缺失 → 状态转 NEED_CLARIFY → 询问用户
```

#### 步骤 5：参数校验（ParameterValidator）

```
输入: 抽取的参数 + 校验规则

校验:
- order_id 格式匹配正则 ORD-\d{4}-\d{6}
- order_id 非空

输出: ValidationResult{valid=true}

失败兜底: 格式错误 → 提示用户重新输入正确格式
```

#### 步骤 6：权限校验（PermissionChecker）

```
输入: userId + order_id

校验:
- 用户是否有权查看该订单
- 执行 SQL: SELECT 1 FROM ORDERS WHERE ORDER_ID=? AND USER_ID=?

输出: 有权限

失败兜底: 无权限 → 返回"您无权查看该订单"
```

#### 步骤 7：工具执行（ToolExecutor → SqlQueryTool）

```
输入: 
  - toolName: "query_logistics"
  - params: {"order_id": "ORD-2026-001234"}

执行预定义 SQL:
  SELECT 
    LOGISTICS_STATUS,
    CURRENT_LOCATION,
    ESTIMATE_ARRIVAL,
    UPDATE_TIME
  FROM V_ORDER_LOGISTICS 
  WHERE ORDER_ID = ?

输出: ToolResult{
  success: true,
  data: {
    "logistics_status": "运输中",
    "current_location": "深圳转运中心",
    "estimate_arrival": "2026-01-18",
    "update_time": "2026-01-16 10:30:00"
  }
}

失败兜底: 
- SQL 执行异常 → 返回系统错误 + 记录日志
- 查询无结果 → 返回"未找到该订单的物流信息"
```

#### 步骤 8：响应生成（LLMClient）

```
输入: 工具执行结果 + 响应模板

LLM Prompt:
"""
请根据以下物流信息，生成自然语言回复：

订单号: ORD-2026-001234
物流状态: 运输中
当前位置: 深圳转运中心
预计送达: 2026-01-18
更新时间: 2026-01-16 10:30:00

要求：
1. 语气友好专业
2. 不要添加任何不在上述信息中的内容
3. 不要编造或推测任何信息
"""

输出: "您好！您的订单 ORD-2026-001234 当前正在运输中，目前已到达深圳转运中心，预计将于2026年1月18日送达。最后更新时间为今天上午10:30。如有其他问题，请随时询问。"

失败兜底: LLM 调用失败 → 使用固定模板填充
```

#### 步骤 9：结果校验 & 响应

```
输入: LLM 生成的回复

校验:
- 敏感词过滤
- 长度检查
- 是否包含禁止内容

输出: 返回给用户

失败兜底: 校验失败 → 使用安全回复替换
```

---

## 第六部分：反幻觉与风控设计

### 6.1 什么时候直接拒答

| 场景 | 检测方式 | 处理 |
|-----|---------|-----|
| 用户询问超出业务范围的问题 | 意图识别为 UNKNOWN + 3次澄清失败 | 拒答 + 引导至正确渠道 |
| 敏感词/违禁词 | SensitiveWordFilter 命中黑名单 | 直接拒答 + 记录审计 |
| 请求包含 SQL 注入/XSS | 参数校验层正则检测 | 拒绝处理 + 安全告警 |
| 请求频率异常 | RiskController 限流策略 | 拦截 + 触发风控 |

### 6.2 什么时候转人工

| 场景 | 触发条件 | 处理 |
|-----|---------|-----|
| 意图无法识别 | 连续3轮 UNKNOWN | 转人工 + 传递上下文 |
| 用户主动要求 | 识别到"转人工"意图 | 立即转接 |
| 工具执行持续失败 | 同一请求重试3次失败 | 转人工 + 技术告警 |
| 情绪异常 | 情绪识别为"愤怒/失望"且持续 | 优先转人工 |
| 复杂业务场景 | 涉及退款/投诉等高风险操作 | 人工介入 |

### 6.3 为什么模型无法编造答案

```
+------------------+------------------+------------------+
|     传统设计     |     本系统设计    |     差异说明     |
+------------------+------------------+------------------+
| LLM 直接回答问题  | LLM 只做意图理解  | LLM 不接触业务数据 |
+------------------+------------------+------------------+
| LLM 自由生成内容  | LLM 填充数据模板  | 输出结构受限      |
+------------------+------------------+------------------+
| 上下文自由拼接    | 结构化历史管理    | 无多余信息干扰    |
+------------------+------------------+------------------+
| 缺失信息靠推断    | 缺失信息问用户    | 不允许推断补全    |
+------------------+------------------+------------------+
| 数据来源不可控    | 数据只来自 Tool   | 可追溯可审计      |
+------------------+------------------+------------------+
```

**核心机制**：

1. **数据隔离**：LLM 永远看不到原始数据库，只能收到 Tool 返回的结构化结果
2. **模板约束**：响应生成时使用严格模板，LLM 只能在占位符处填充
3. **结果校验**：LLM 生成的内容经过后校验，包含非法信息则替换为安全回复
4. **审计追踪**：每次 LLM 调用的输入输出均记录，可回溯分析

---

## 附录：快速启动

```bash
# 1. 克隆项目
cd c:\work\jt-agent\agent-service

# 2. 修改配置
# 编辑 src/main/resources/application.yml

# 3. 构建运行
mvn clean package
java -jar target/agent-service.jar

# 4. 测试接口
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"test","userId":"user001","message":"查询订单状态"}'
```
