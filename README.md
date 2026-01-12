# 智慧运力智能客服 Agent Demo

这是一个基于 Python 和 LangChain 框架的简单智能客服 Agent 演示。

## 目录结构
- `agent.py`: 主程序代码
- `requirements.txt`: 依赖库列表
- `.env.example`: 环境变量配置模板

## 快速开始

### 1. 环境准备
确保已安装 Python 3.8 或更高版本。

### 2. 安装依赖
在终端运行以下命令安装所需依赖：
```bash
pip install -r requirements.txt
```

### 3. 配置 API Key
1. 将 `.env.example` 文件复制或重命名为 `.env`。
2. 打开 `.env` 文件，填入你的 OpenAI API Key (或其他兼容 OpenAI 格式的大模型 API Key)。
   ```ini
   OPENAI_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxx
   OPENAI_API_BASE=https://api.openai.com/v1  # 如使用其他服务商(如DeepSeek/阿里/百度)，请修改此URL
   MODEL_NAME=gpt-3.5-turbo # 根据实际使用的模型名称修改
   ```

### 4. 运行 Agent
在终端运行：
```bash
python agent.py
```

### 5. 使用
启动后，直接在终端输入问题即可与 Agent 对话。输入 `q` 或 `exit` 退出。

## 扩展方向
- **接入业务数据**: 可以添加 LangChain Tools 来查询数据库（如查询标书状态、运力资源）。
- **RAG (检索增强生成)**: 集成 `yl-web-transport-cooperate` 中的文档（如白皮书），让 Agent 能回答具体的业务规则问题。
