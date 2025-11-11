from langchain.chat_models import init_chat_model
from app.core.config import get_settings
from app.schemas.agents import LLMResponse
import os


class Models:
    """
    ## LLM 모델 정의
    - init : 환경변수 정의
    - summarize_model : 요약 에이전트 선언
    """
    def __init__(self):
        self.settings = get_settings()
        os.environ["OPENAI_API_KEY"] = self.settings.openai_api_key
        os.environ["OPENAI_API_BASE"] = self.settings.openai_base_url

    def summarize_model(self):
        model = init_chat_model(
            model=self.settings.summarize_model,
            temperature=self.settings.summarize_temperature,
        )
        structured_model = model.with_structured_output(LLMResponse)
        return structured_model
