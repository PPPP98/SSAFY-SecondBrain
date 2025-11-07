from langchain.agents import create_agent
from app.core.config import get_settings
from app.agents.note_summarize_agent.prompts import SummarizePrompts
import os


settings = get_settings()
os.environ["OPENAI_API_KEY"] = settings.openai_api_key
os.environ["OPENAI_API_BASE"] = settings.openai_base_url

agent = create_agent(
    model=settings.openai_model,
    system_prompt=SummarizePrompts.SYSTEMPROMPT,
)
