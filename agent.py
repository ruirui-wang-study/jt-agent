import os
import sys
from dotenv import load_dotenv
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser

# 加载环境变量
load_dotenv()

import os
import sys
from dotenv import load_dotenv
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser
from langchain_core.tools import tool
# from langchain.agents import AgentExecutor
from sqlalchemy import create_engine, text
import oracledb

# 加载环境变量
load_dotenv(override=True)

# --- 数据库连接 ---
def get_db_engine():
    user = os.getenv("DB_USER")
    password = os.getenv("DB_PASSWORD")
    host = os.getenv("DB_HOST", "127.0.0.1")
    port = os.getenv("DB_PORT", "1521")
    service_name = os.getenv("DB_SERVICE_NAME", "helowin")
    
    if not user or not password:
        return None

    dsn = f"{host}:{port}/{service_name}"
    connection_string = f"oracle+oracledb://{user}:{password}@{dsn}"
    
    try:
        engine = create_engine(connection_string)
        return engine
    except Exception as e:
        print(f"Database connection error: {e}")
        return None

# --- 定义工具 (Tools) ---

@tool
def check_tender_result(tender_id: str) -> str:
    """
    主要用于查询标书的中标结果。
    
    Args:
        tender_id: 标书编号/ID (例如: T20240101)
    """
    engine = get_db_engine()
    if not engine:
        return "数据库连接未配置，请联系管理员配置 DB_USER 和 DB_PASSWORD。"

    try:
        with engine.connect() as connection:
            # 查询标书状态
            # BID_STATUS: 3-招标中, 4-已结束/中标? (需根据实际业务确认status含义，这里假设)
            # 根据Mapper: BID_STATUS=4 AND WIN_CARRIER_ID IS NOT NULL -> 已中标
            query = text("""
                SELECT TENDER_NO, LINE_NAME, BID_STATUS, WIN_CARRIER_ID
                FROM YL_TMS_TEMP_CAR_TENDER 
                WHERE TENDER_NO = :tender_id
            """)
            result = connection.execute(query, {"tender_id": tender_id}).fetchone()
            
            if not result:
                return "未找到该标书编号，请确认编号是否正确。"
            
            tender_no, line_name, status, win_carrier_id = result
            
            # 简单的状态映射
            status_map = {
                1: "待发布",
                2: "已发布/竞价中",
                3: "待开标",
                4: "已定标(结束)"
            }
            status_str = status_map.get(status, f"未知状态({status})")
            
            if status == 4 and win_carrier_id:
                return f"标书 [{tender_no}] ({line_name}) 已定标。中标承运商ID: {win_carrier_id}。"
            elif status == 4:
                return f"标书 [{tender_no}] ({line_name}) 已结束，但未显示中标承运商（可能流标）。"
            else:
                return f"标书 [{tender_no}] ({line_name}) 当前状态: {status_str}。"
                
    except Exception as e:
        return f"查询数据库时发生错误: {e}"

@tool
def list_my_tenders(carrier_id: str = None) -> str:
    """
    列出承运商参与的最近5个标书。
    
    Args:
        carrier_id: 承运商ID。如果未提供，Agent会尝试查询默认或提示用户。
    """
    if not carrier_id:
        return "请提供承运商ID以查询其参与的标书。"
        
    engine = get_db_engine()
    if not engine:
        return "数据库连接未配置。"
        
    try:
        with engine.connect() as connection:
            query = text("""
                SELECT t.TENDER_NO, t.LINE_NAME, t.BID_STATUS, h.OFFER_PRICE, h.WIN_BID 
                FROM YL_TMS_TEMP_CAR_CARRIER_OFFER h
                JOIN YL_TMS_TEMP_CAR_TENDER t ON h.TENDER_NO = t.TENDER_NO
                WHERE h.CARRIER_ID = :carrier_id
                ORDER BY h.CREATE_TIME DESC
                FETCH FIRST 5 ROWS ONLY
            """)
            result = connection.execute(query, {"carrier_id": carrier_id}).fetchall()
            
            if not result:
                return f"未找到承运商 [{carrier_id}] 的参与记录。"
            
            response = "您最近参与的标书:\n"
            for row in result:
                t_no, line, status, price, is_win = row
                win_str = "已中标" if is_win == 1 else "未中标"
                response += f"- [{t_no}] {line}: 报价 {price}, 状态: {win_str}\n"
            return response
            
    except Exception as e:
        return f"查询数据库时发生错误: {e}"


from langchain_core.messages import SystemMessage, HumanMessage, ToolMessage

# ... (Previous code remains, but we will overwrite get_agent_executor and main)

class SimpleAgent:
    def __init__(self, llm, tools):
        self.llm = llm
        self.tools = {t.name: t for t in tools}
        self.llm_with_tools = llm.bind_tools(tools)
        self.system_message = SystemMessage(content="你是一个智慧运力平台的智能客服助手'运力小助手'。你需要回答货主或司机关于运力招采、标书状态的问题。\n"
                       "你可以使用工具来查询具体的业务数据。如果用户没有提供标书ID，请先引导用户提供或先调用工具查询列表。")

    def invoke(self, inputs):
        user_input = inputs.get("input")
        messages = [self.system_message, HumanMessage(content=user_input)]
        
        # Max steps to prevent infinite loops
        for _ in range(10):
            ai_msg = self.llm_with_tools.invoke(messages)
            messages.append(ai_msg)
            
            if not ai_msg.tool_calls:
                return {"output": ai_msg.content}
            
            for tool_call in ai_msg.tool_calls:
                tool_name = tool_call["name"]
                args = tool_call["args"]
                tool_func = self.tools.get(tool_name)
                
                print(f"[调用工具] {tool_name} 参数: {args}")
                
                if tool_func:
                    try:
                        # tool.invoke accepts a dict or args depending on impl, tools created with @tool accept args directly or dict
                        tool_result = tool_func.invoke(args)
                    except Exception as e:
                        tool_result = f"Error executing tool: {e}"
                else:
                    tool_result = f"Error: Tool {tool_name} not found"
                
                print(f"[工具返回] {tool_result[:200]}..." if len(str(tool_result)) > 200 else f"[工具返回] {tool_result}")

                messages.append(ToolMessage(tool_call_id=tool_call["id"], content=str(tool_result)))
        
        return {"output": messages[-1].content}

def get_agent_executor():
    """
    初始化并返回一个支持工具调用的 Agent (手动实现版)
    """
    api_key = os.getenv("OPENAI_API_KEY")
    base_url = os.getenv("OPENAI_API_BASE")
    model_name = os.getenv("MODEL_NAME", "gpt-3.5-turbo")

    if not api_key or "your_api_key" in api_key:
        print("错误: 请在 .env 文件中配置有效的 OPENAI_API_KEY")
        return None

    # 1. 初始化 LLM
    llm = ChatOpenAI(
        api_key=api_key,
        base_url=base_url,
        model=model_name,
        temperature=0.3
    )

    # 2. 定义工具列表
    tools = [check_tender_result, list_my_tenders]

    # 3. 返回简单的手动Agent
    return SimpleAgent(llm, tools)


def main():
    print("正在初始化智慧运力智能客服 Agent (带业务工具 - Manual Mode)...")
    agent_executor = get_agent_executor()
    
    if not agent_executor:
        return

    print("\n=== 智慧运力智能客服 (输入 'q' 退出) ===")
    print("支持的问题示例: '我的标书中标了吗？', '查询 T2023001 的状态'")

    while True:
        try:
            user_input = input("\n用户: ").strip()
            
            if user_input.lower() in ['q', 'quit', 'exit', '退出']:
                print("再见！")
                break
            
            if not user_input:
                continue

            print("Agent: (思考中...)")
            
            # 使用 invoke 调用 Executor
            response = agent_executor.invoke({"input": user_input})
            
            print(f"Agent: {response['output']}\n")

        except KeyboardInterrupt:
            print("\n再见！")
            break
        except Exception as e:
            print(f"\n发生错误: {e}")

if __name__ == "__main__":
    main()
