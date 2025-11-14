from langchain.chat_models import init_chat_model
from app.core.config import get_settings

from app.schemas.agents import PreFilterOutput, RelevanceCheckOutput
import os


class Models:
    """
    ## LLM 모델 정의
    - init : 환경변수 정의
    - get_rewrite_model : 쿼리 재작성 에이전트
    - get_related_check_model : content 내용 관련성 체크 에이전트
    - get_result_model : 최종 응답 생성 에이전트
    """

    def __init__(self):
        self.settings = get_settings()
        os.environ["OPENAI_API_KEY"] = self.settings.openai_api_key
        os.environ["OPENAI_API_BASE"] = self.settings.openai_base_url

    def get_prefilter_model(self):
        """Pre-filter용 structured output 모델"""
        model = init_chat_model(
            model=self.settings.search_agent_model,
            temperature=self.settings.search_agent_temperature,
        )
        return model.with_structured_output(PreFilterOutput)

    def get_relevance_check_model(self):
        """연관성 체크용 structured output 모델"""
        model = init_chat_model(
            model=self.settings.search_agent_model,
            temperature=self.settings.search_agent_temperature,
        )
        return model.with_structured_output(RelevanceCheckOutput)

    def get_response_model(self):
        """응답 생성용 일반 모델"""
        model = init_chat_model(
            model=self.settings.search_agent_model,
            temperature=self.settings.search_agent_temperature,
        )
        return model
